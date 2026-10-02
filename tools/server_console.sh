#!/usr/bin/env bash
# Runs the dev server headless, types console commands into it, and prints what the server says.
#
#   tools/server_console.sh tools/server_tests/<name>.txt
#
# The commands file has one console command per line (no leading slash). "sleep <seconds>" pauses;
# blank lines and lines starting with # are skipped. The server is stopped at the end.
#
# Gradle doesn't pass console input through to runServer, so the script captures the server's
# java command line once (from a normal runServer start) and reruns it with the input piped in.
# Linux only (it reads /proc).
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMMANDS="$(realpath "$1")"
CMDLINE="$ROOT/build/server-cmdline.bin"
LOG="$ROOT/build/server-console.log"
cd "$ROOT"

./gradlew classes --console=plain -q || exit 1

if [[ ! -s "$CMDLINE" || "$ROOT/build.gradle" -nt "$CMDLINE" ]]; then
  echo "Capturing the dev server's launch command..." >&2
  CAPTURE_LOG="$ROOT/build/server-capture.log"
  (timeout 180 ./gradlew runServer --console=plain > "$CAPTURE_LOG" 2>&1 &)
  timeout 180 bash -c "until grep -qE 'Done \(|Failed to start' '$CAPTURE_LOG'; do sleep 1; done"
  for pid in $(pgrep -x java); do
    if tr '\0' ' ' < "/proc/$pid/cmdline" | grep -q "devlaunch.Main"; then
      # Not cp: it would copy /proc's read-only mode, and the next capture couldn't overwrite it.
      chmod u+w "$CMDLINE" 2>/dev/null
      cat "/proc/$pid/cmdline" > "$CMDLINE"
      kill "$pid"
      while kill -0 "$pid" 2>/dev/null; do sleep 0.5; done
    fi
  done
  [[ -s "$CMDLINE" ]] || { echo "Could not capture the server command (see $CAPTURE_LOG)" >&2; exit 1; }
fi

mapfile -d '' ARGS < "$CMDLINE"
FIFO="$(mktemp -u)"
mkfifo "$FIFO"
(cd "$ROOT/run-server" && exec timeout 300 "${ARGS[@]}" < "$FIFO" > "$LOG" 2>&1) &
SERVER=$!
exec 3>"$FIFO"
timeout 180 bash -c "until grep -qE 'Done \(|Failed to start' '$LOG'; do sleep 1; done"

while IFS= read -r line || [[ -n "$line" ]]; do
  [[ -z "$line" || "$line" == \#* ]] && continue
  if [[ "$line" == sleep\ * ]]; then
    sleep "${line#sleep }"
  else
    echo "$line" >&3
    sleep 0.5
  fi
done < "$COMMANDS"
echo "stop" >&3
exec 3>&-
wait "$SERVER"
rm -f "$FIFO"

# Everything the server said after starting, minus startup/shutdown noise.
sed -n '/Done (/,$p' "$LOG" \
  | grep -E '\[Server thread/(INFO|WARN|ERROR)\]|Exception' \
  | grep -vE 'Done \(|Stopping|Saving|saved|ThreadedAnvilChunkStorage|Loaded [0-9]+ language|Gametest|PermissionAPI|DualStackUtils' \
  | sed -E 's/^\[[0-9:]+\] \[Server thread\/[A-Z]+\] \[[^]]*\]: //'
