#!/usr/bin/env bash
# Runs CarHome in the emulator at a car-screen size and captures real screenshots.
# Called by .github/workflows/carhome-screenshots.yml (working dir: carhome/).
set -x
PKG=com.carhome.launcher
OUT=shots
mkdir -p "$OUT"

adb wait-for-device
adb shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'

# 1280x720 landscape at ~mdpi: what most CarPlay AI boxes output to the car screen
adb shell wm size 1280x720
adb shell wm density 160
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0.5
# Skip Android's one-time "Viewing full screen" notice so it doesn't cover the shots
adb shell settings put secure immersive_mode_confirmations confirmed

adb install -r app/build/outputs/apk/debug/app-debug.apk

# Grant what the user would grant on first launch
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb shell cmd notification allow_listener $PKG/$PKG.MediaService || true

# Fill 7 tiles with apps that exist on the emulator (8th left empty to show "Add app")
APPS=$(adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
  | tr -d '\r' | grep '/' | cut -d/ -f1 | sed 's/^ *//' | sort -u \
  | grep -E 'chrome|youtube|maps|photos|gm$|messaging|dialer|calendar|camera|deskclock|music|settings' \
  | grep -v carhome | head -7 | tr '\n' '|')
echo "Tiles: $APPS"
cat > /tmp/carhome.xml <<EOF
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="tiles">${APPS}</string>
    <int name="tile_count" value="8" />
    <boolean name="asked_location" value="true" />
</map>
EOF
adb push /tmp/carhome.xml /data/local/tmp/carhome.xml
adb shell "run-as $PKG mkdir -p shared_prefs"
adb shell "run-as $PKG cp /data/local/tmp/carhome.xml shared_prefs/carhome.xml"

# Al-Ahsa (Hofuf) — so the weather is real for your area
adb emu geo fix 49.5886 25.3833
adb shell am start -n $PKG/.MainActivity
sleep 4
adb emu geo fix 49.5887 25.3834
sleep 14   # let the weather download
adb exec-out screencap -p > "$OUT/1-home.png"

# Simulate driving: emulator "geo fix <lon> <lat> <alt> <satellites> <velocity-knots>"
# 70 knots ≈ 130 km/h — over the default 120 limit, so the alert shows
for i in 1 2 3 4 5 6 7; do
  adb emu geo fix 49.59$((10 + i)) 25.3833 150 8 70
  sleep 1
done
adb exec-out screencap -p > "$OUT/2-home-speed-alert.png"
for i in 1 2 3; do adb emu geo fix 49.5920 25.3833 150 8 0; sleep 1; done
sleep 3

# Settings page (Settings button: top-right corner)
adb shell input tap 1232 48
sleep 2
adb exec-out screencap -p > "$OUT/3-settings.png"
adb shell input keyevent KEYCODE_BACK
sleep 2

# All apps page (button left of Settings)
adb shell input tap 1164 48
sleep 2
adb exec-out screencap -p > "$OUT/4-all-apps.png"
adb shell input keyevent KEYCODE_BACK
sleep 2

# Arabic (per-app language, Android 13+)
adb shell cmd locale set-app-locales $PKG --locales ar || true
adb shell am force-stop $PKG
adb shell am start -n $PKG/.MainActivity
sleep 12
adb exec-out screencap -p > "$OUT/5-home-arabic.png"

ls -la "$OUT"
