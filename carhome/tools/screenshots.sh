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

# Simulate driving at ~130 km/h (70 knots) with NMEA sentences to show the speed alert
nmea() {
  python3 - "$1" "$2" <<'PY'
import sys, time
lat, kn = float(sys.argv[1]), sys.argv[2]
t = time.gmtime()
body = "GPRMC,%s,A,%02d%07.4f,N,%03d%07.4f,E,%s,90.0,%s,,," % (
    time.strftime("%H%M%S", t), int(lat), (lat % 1) * 60, 49, 0.5886 * 60, kn, time.strftime("%d%m%y", t))
cs = 0
for ch in body: cs ^= ord(ch)
print("$%s*%02X" % (body, cs))
PY
}
for i in 1 2 3 4 5 6; do
  adb emu geo nmea "$(nmea 25.38$((30 + i)) 70.0)"
  sleep 1
done
adb exec-out screencap -p > "$OUT/2-home-speed-alert.png"
for i in 1 2 3; do adb emu geo nmea "$(nmea 25.3840 0.0)"; sleep 1; done

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
