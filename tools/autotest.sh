#!/usr/bin/env bash
# Plays a scripted test in the game client, hidden in a virtual KDE session (nothing shows on the
# real screen, and the game's sound is muted), and leaves its screenshots in run-autotest/screenshots.
#
#   tools/autotest.sh tools/autotest/<name>.txt
#
# The script's steps are read by client/AutoTest (see its Javadoc for the step list). The test
# world is a copy of the dev server's world (run-server/world), made on first use.
#
# With EA_AUTOTEST_SERVER=<host> set (tools/mp_test.sh sets it), the client joins that server
# instead of loading the test world.
#
# The hidden session is cut off from the desktop: everything here runs on a D-Bus session of its
# own that can start no services (tools/autotest-dbus.conf), without the desktop's display, and
# the hidden KWin reads an empty config directory. A second KWin on the desktop's bus, reading the
# desktop's config, loads the desktop's KWin scripts and shortcuts and registers them with the
# desktop's shortcut service; when it quits they're left switched off, and the desktop's own KWin
# shortcuts stop working until the next login. A bus that can start services wakes portals and
# file services of its own beside the desktop's.
set -uo pipefail
if [[ -z "${EA_AUTOTEST_ISOLATED:-}" ]]; then
  exec env -u WAYLAND_DISPLAY -u DISPLAY EA_AUTOTEST_ISOLATED=1 \
    dbus-run-session --config-file="$(dirname "$0")/autotest-dbus.conf" -- "$0" "$@"
fi
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPT="$(realpath "$1")"
GAME="$ROOT/run-autotest"
LOG="$ROOT/build/autotest.log"
mkdir -p "$GAME/saves" "$GAME/screenshots" "$ROOT/build"
cp "$SCRIPT" "$GAME/autotest.txt"
if [[ ! -d "$GAME/saves/eatest" ]]; then
  cp -r "$ROOT/run-server/world" "$GAME/saves/eatest"
  rm -f "$GAME/saves/eatest/session.lock"
fi
if [[ ! -f "$GAME/options.txt" ]]; then
  cat > "$GAME/options.txt" <<'OPTIONS'
version:3955
onboardAccessibility:false
pauseOnLostFocus:false
renderDistance:8
simulationDistance:6
guiScale:2
fullscreen:false
soundCategory_master:0.0
narrator:0
tutorialStep:none
skipMultiplayerWarning:true
joinedFirstServer:true
OPTIONS
fi
rm -f "$GAME/screenshots/"*.png
cd "$ROOT"

# A virtual (invisible) KDE compositor for the game to draw into, with a config of its own (see
# the note at the top).
SOCKET="ea-autotest"
KWIN_CONFIG="$ROOT/build/autotest-kwin-config"
mkdir -p "$KWIN_CONFIG"
XDG_CONFIG_HOME="$KWIN_CONFIG" kwin_wayland --virtual --socket "$SOCKET" --width 1280 --height 720 --no-lockscreen \
  --no-global-shortcuts > "$ROOT/build/autotest-kwin.log" 2>&1 &
KWIN=$!
for _ in $(seq 1 50); do [[ -S "$XDG_RUNTIME_DIR/$SOCKET" ]] && break; sleep 0.2; done
# No DISPLAY: the game can only reach the hidden Wayland compositor, never the real screen. The
# virtual compositor can't take NVIDIA's buffers, so the game renders with Mesa's software OpenGL.
env -u DISPLAY WAYLAND_DISPLAY="$SOCKET" XDG_SESSION_TYPE=wayland \
  __EGL_VENDOR_LIBRARY_FILENAMES=/usr/share/glvnd/egl_vendor.d/50_mesa.json __GLX_VENDOR_LIBRARY_NAME=mesa \
  LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe timeout 900 \
  ./gradlew runAutotest --no-daemon --console=plain -q ${EA_AUTOTEST_SERVER:+-PautotestServer=$EA_AUTOTEST_SERVER} > "$LOG" 2>&1
kill "$KWIN" 2>/dev/null
wait "$KWIN" 2>/dev/null
echo "autotest finished (log: build/autotest.log)"
ls "$GAME/screenshots" 2>/dev/null
