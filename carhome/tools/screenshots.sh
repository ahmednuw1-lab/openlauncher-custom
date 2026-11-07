#!/usr/bin/env bash
# Runs CarHome in the emulator at a car-screen size and captures real screenshots.
# Called by .github/workflows/carhome-screenshots.yml (working dir: carhome/).
set -x
PKG=com.carhome.launcher
OUT=shots
mkdir -p "$OUT"
shot() { adb exec-out screencap -p > "$OUT/$1.png"; }

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

# CarHome is the home screen; switch off the emulator's own launcher so its
# "isn't responding" dialogs (slow software graphics) can't cover the shots
adb shell cmd package set-home-activity $PKG/.MainActivity || true
adb shell pm disable-user --user 0 com.google.android.apps.nexuslauncher || true

# Grant what the user would grant on first launch
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb shell cmd notification allow_listener $PKG/$PKG.MediaService || true
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow || true

# Apps that exist on the emulator
ALL=$(adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
  | tr -d '\r' | grep '/' | cut -d/ -f1 | sed 's/^ *//' | sort -u \
  | grep -E 'chrome|youtube|maps|photos|gm$|messaging|dialer|calendar|camera|deskclock|music|settings' \
  | grep -v carhome)
APPS7=$(echo "$ALL" | head -7 | tr '\n' '|')
APPS6=$(echo "$ALL" | head -6 | tr '\n' '|')
SMART_APP=$(echo "$ALL" | sed -n 8p)
[ -z "$SMART_APP" ] && SMART_APP=$(echo "$ALL" | tail -1)
BOOT=$(adb shell settings get global boot_count | tr -d '\r')
echo "Tiles: $APPS7  smart: $SMART_APP  boot: $BOOT"

# Writes CarHome's saved state. $1 = settings JSON, $2 = tiles, $3 = boot count seen
prefs() {
  cat > /tmp/carhome.xml <<EOF
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="settings">$1</string>
    <string name="tiles">$2</string>
    <string name="places">[{"id":"home1","name":"Home","address":"","lat":25.3990,"lon":49.5800}]</string>
    <boolean name="asked_location" value="true" />
    <int name="boot_count" value="$3" />
    <string name="trip">{"start":0,"dist":23400,"moving":1680000,"max":33.5,"harsh":2}</string>
    <string name="last_trip">{"start":0,"dist":12800,"moving":960000,"max":27.0,"harsh":0}</string>
    <string name="launches">{"$SMART_APP":[9,9,9,9,9,9,9,9]}</string>
    <string name="odometer_km">10450.0</string>
    <string name="oil_at_km">2000.0</string>
</map>
EOF
  adb shell am force-stop $PKG
  adb push /tmp/carhome.xml /data/local/tmp/carhome.xml
  adb shell "run-as $PKG mkdir -p shared_prefs"
  adb shell "run-as $PKG cp /data/local/tmp/carhome.xml shared_prefs/carhome.xml"
}
start() { adb shell am start -n $PKG/.MainActivity; }
# Emulator "geo fix <lon> <lat> <alt> <satellites> <velocity-knots>"; 70 knots ≈ 130 km/h
drive() { for i in $(seq 1 "$1"); do adb emu geo fix 49.59$((10 + i)) 25.3833 150 8 70; sleep 1; done; }
park() { for i in 1 2 3; do adb emu geo fix 49.5920 25.3833 150 8 0; sleep 1; done; }

DEFAULTS='{"tileCount":8}'
CUSTOM='{"tileCount":8,"cards":["CLOCK","SPEED","PRAYER","TRIP"],"showWeather":true,"showHijri":true,"background":"SKY","placeTiles":true,"smartTile":true,"driveScore":true,"fuelCost":true,"oilReminder":true}'
CTILES="${APPS6}place:home1|smart"

# Al-Ahsa (Hofuf) — so weather and prayer times are real for your area
adb emu geo fix 49.5886 25.3833

# 1–2: the default look (same as 1.1), then over the speed limit
prefs "$DEFAULTS" "$APPS7" "$BOOT"
start
sleep 4
adb emu geo fix 49.5887 25.3834
sleep 14
shot 1-home
drive 7
shot 2-home-speed-alert
park
sleep 3

# 3: customised home — clock with Hijri date, speed, prayer and trip cards, living sky, place and smart tiles
prefs "$CUSTOM" "$CTILES" "$BOOT"
start
sleep 4
adb emu geo fix 49.5886 25.3833
sleep 22
shot 3-home-custom

# 4: floating speed bubble over another app while over the limit
prefs "${CUSTOM%\}},\"bubble\":true,\"bubbleSize\":\"LARGE\"}" "$CTILES" "$BOOT"
start
sleep 6
drive 5
# (Android hides other apps' bubbles on its own Settings screens, so use the Clock app)
adb shell am start -a android.intent.action.SHOW_ALARMS || adb shell am start -a android.intent.action.VIEW -d https://example.com
drive 9
shot 4-bubble-over-app
park
start
sleep 3

# 5–8: Settings pages (side menu items are 60 px apart, first at y=130)
adb shell input tap 1232 48
sleep 2
shot 5-settings-home
adb shell input tap 145 190
sleep 2
shot 6-settings-speed
adb shell input tap 145 250
sleep 2
shot 7-settings-prayer
adb shell input tap 145 490
sleep 2
shot 8-settings-smart
adb shell input keyevent KEYCODE_BACK
sleep 1

# 9: welcome screen (a changed boot count looks like the car just started)
prefs "${CUSTOM%\}},\"welcome\":true,\"welcomeName\":\"Ahmed\"}" "$CTILES" "-5"
start
sleep 8
shot 9-welcome
sleep 6

# 10: parked screen after 2 minutes without touching
prefs "${CUSTOM%\}},\"parkedScreen\":true,\"parkedAfterMin\":2}" "$CTILES" "$BOOT"
start
sleep 135
shot 10-parked

# 11: Arabic (per-app language, Android 13+)
adb shell cmd locale set-app-locales $PKG --locales ar || true
prefs "$CUSTOM" "$CTILES" "$BOOT"
start
sleep 14
shot 11-home-arabic
adb shell input tap 48 48
sleep 2
shot 12-settings-arabic

# 13: a wide, low car screen (1920x720 at 240 dpi = 1280x480 dp), with four cards
adb shell cmd locale set-app-locales $PKG --locales ar || true
adb shell wm size 1920x720
adb shell wm density 240
prefs '{"tileCount":8,"cards":["CLOCK","SPEED","MUSIC","CAR"],"showHijri":true}' "$APPS7" "$BOOT"
start
sleep 14
shot 13-short-screen-arabic
drive 6
shot 14-short-screen-speed
park

# Crash and CarHome log lines, to catch problems a screenshot can't show
adb logcat -d -b crash > "$OUT/crash-log.txt" || true
adb logcat -d | grep -E "AndroidRuntime|CarHome|carhome" | tail -300 > "$OUT/app-log.txt" || true
ls -la "$OUT"
