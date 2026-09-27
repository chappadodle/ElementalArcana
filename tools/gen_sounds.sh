#!/usr/bin/env bash
# Synthesizes the mod's custom sounds with sox (no samples, nothing to license).
# Run from the project root:  bash tools/gen_sounds.sh
set -euo pipefail
# awk would otherwise print decimals with the system locale's separator (e.g. "1,14").
export LC_ALL=C

SOUNDS="src/main/resources/assets/elementalarcana/sounds"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$SOUNDS/magic" "$SOUNDS/spell"

RATE=(-r 44100 -c 1)

# Rising arpeggio: chime <name> <note length> <gap between notes> <frequencies...>
chime() {
  local name=$1 length=$2 gap=$3
  shift 3
  local i=0
  for freq in "$@"; do
    delay=$(awk "BEGIN { print $i * $gap }")
    sox -n "${RATE[@]}" "$TMP/$name$i.wav" synth "$length" sine "$freq" sine "$(awk "BEGIN { print $freq * 2 }")" \
      remix 1,2 fade q 0.005 "$length" "$(awk "BEGIN { print $length * 0.95 }")" gain -n -6 pad "$delay"
    i=$((i + 1))
  done
  sox -m "$TMP/$name"*.wav "${RATE[@]}" "$TMP/$name.wav" reverb 50 gain -n -3
}

# awaken: a low swell under a bright C-E-G chime.
chime awaken_notes 1.2 0.12 1046.5 1318.5 1568.0
sox -n "${RATE[@]}" "$TMP/swell.wav" synth 1.6 sine 130.8 sine 196 remix 1,2 fade t 0.6 1.6 0.8 gain -n -12
sox -m "$TMP/awaken_notes.wav" "$TMP/swell.wav" "${RATE[@]}" "$SOUNDS/magic/awaken.ogg" gain -n -2

# level_up: a quicker four-note fanfare up to the octave.
chime level_up 0.7 0.07 783.99 987.77 1174.66 1567.98
sox "$TMP/level_up.wav" "${RATE[@]}" "$SOUNDS/magic/level_up.ogg"

# fizzle: a short falling "pew" over a crackle of filtered noise.
sox -n "${RATE[@]}" "$TMP/pew.wav" synth 0.25 sine 650-180 fade q 0.005 0.25 0.2 gain -n -8
sox -n "${RATE[@]}" "$TMP/crackle.wav" synth 0.35 brownnoise fade q 0.01 0.35 0.3 lowpass 1500 tremolo 35 70 gain -n -10
sox -m "$TMP/pew.wav" "$TMP/crackle.wav" "${RATE[@]}" "$SOUNDS/spell/fizzle.ogg" gain -n -3

echo "wrote magic/awaken.ogg, magic/level_up.ogg, spell/fizzle.ogg"
