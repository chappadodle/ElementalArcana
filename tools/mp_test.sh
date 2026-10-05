#!/usr/bin/env bash
# Plays a scripted test as a client joined to the dev's dedicated server, the way a multiplayer
# game is played, to catch what a single-player test can't: a client-only class touched on the
# server, a payload one side can't read, a particle or screen that only works when client and
# server share a process.
#
#   tools/mp_test.sh tools/autotest/<name>.txt
#
# The dedicated server (run-server, offline mode, as tools/server_console.sh starts it) comes up
# first, with the dev client's name ("Dev") made an operator so the script's `cmd` steps can run;
# then the hidden client (tools/autotest.sh, with -PautotestServer) joins it and plays the script;
# then the server is stopped. Both logs are checked for errors: build/mp-server.log, and the
# client's build/autotest.log.
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPT="$(realpath "$1")"
CMDLINE="$ROOT/build/server-cmdline.bin"
LOG="$ROOT/build/mp-server.log"
cd "$ROOT"

./gradlew classes --console=plain -q || exit 1
# The server's launch command, captured as tools/server_console.sh captures it (it reuses one it has).
if [[ ! -s "$CMDLINE" || "$ROOT/build.gradle" -nt "$CMDLINE" ]]; then
  timeout 600 tools/server_console.sh /dev/null > /dev/null 2>&1
fi
[[ -s "$CMDLINE" ]] || { echo "No server launch command (run tools/server_console.sh once)" >&2; exit 1; }

# "Dev" as an operator: offline-mode players' UUIDs come from their names.
python3 - "$ROOT/run-server/ops.json" <<'PY'
import hashlib, json, sys, uuid
digest = bytearray(hashlib.md5(b"OfflinePlayer:Dev").digest())
digest[6] = (digest[6] & 0x0F) | 0x30
digest[8] = (digest[8] & 0x3F) | 0x80
dev = str(uuid.UUID(bytes=bytes(digest)))
path = sys.argv[1]
try:
    ops = json.load(open(path))
except (OSError, ValueError):
    ops = []
if not any(op.get("name") == "Dev" for op in ops):
    ops.append({"uuid": dev, "name": "Dev", "level": 4, "bypassesPlayerLimit": False})
    json.dump(ops, open(path, "w"), indent=2)
PY

mapfile -d '' ARGS < "$CMDLINE"
FIFO="$(mktemp -u)"
mkfifo "$FIFO"
(cd "$ROOT/run-server" && exec timeout 1200 "${ARGS[@]}" < "$FIFO" > "$LOG" 2>&1) &
SERVER=$!
exec 3>"$FIFO"
if ! timeout 180 bash -c "until grep -qE 'Done \(|Failed to start' '$LOG'; do sleep 1; done" || grep -q 'Failed to start' "$LOG"; then
  echo "The server didn't start (see $LOG)" >&2
  echo "stop" >&3; exec 3>&-; wait "$SERVER"; rm -f "$FIFO"; exit 1
fi

EA_AUTOTEST_SERVER=localhost tools/autotest.sh "$SCRIPT"
CLIENT=$?

echo "stop" >&3
exec 3>&-
wait "$SERVER"
rm -f "$FIFO"

errors_server=$(sed -n '/Done (/,$p' "$LOG" | grep -cE 'Exception|/ERROR\]')
errors_client=$(grep -E 'Exception|/ERROR\]' "$ROOT/build/autotest.log" | grep -cvE 'GL ERROR|Pre render|Wayland|PipelineManager')
joined=$(grep -c 'Dev joined the game' "$LOG")
echo "client rc=$CLIENT joined=$joined server errors=$errors_server client errors=$errors_client"
[[ $CLIENT -eq 0 && $joined -ge 1 && $errors_server -eq 0 && $errors_client -eq 0 ]]
