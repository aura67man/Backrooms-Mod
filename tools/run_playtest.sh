#!/bin/bash
# Runs the automated playtest headlessly: virtual display, software OpenGL, and OpenAL writing
# everything the game plays into build/playtest-audio.wav (proof that sounds really play).
cd "$(dirname "$0")/.." && . ./env.sh
mkdir -p build
cat > build/alsoft.conf <<CONF
[general]
drivers = wave
[wave]
file = $PWD/build/playtest-audio.wav
CONF
export XDG_RUNTIME_DIR="$PWD/build/xdg-runtime" && mkdir -p "$XDG_RUNTIME_DIR" && chmod 700 "$XDG_RUNTIME_DIR"
export ALSOFT_CONF="$PWD/build/alsoft.conf" LIBGL_ALWAYS_SOFTWARE=1 BACKROOMS_HEADLESS_GL=1
exec xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew runClientGameTest --no-daemon "$@"
