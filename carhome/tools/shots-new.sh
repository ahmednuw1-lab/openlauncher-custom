# Screenshots of what is new in CarHome 1.3.0: the modern and custom home screens.
# Sourced by screenshots.sh after its setup (permissions, the prefs/start/drive/park helpers).
# Taken on a wide, low car screen like the AI box (1920x720 at 240 dpi = 1280x480 dp).

adb shell wm size 1920x720
adb shell wm density 240

SEEN='<int name="last_version_code" value="16" />'
DUE=$(( ($(date +%s) + 12 * 86400) * 1000 ))
REMIND="<string name=\"car_reminders\">[{\"id\":\"r1\",\"type\":\"INSURANCE\",\"name\":\"\",\"date\":$DUE}]</string>"
FAVS="${APPS7}place:home1|smart"

# 40: modern, shortcuts under the clock (place, suggested app, a reminder due soon)
MODERN='{"homeStyle":"MODERN","modernFill":"SHORTCUTS","favCount":9,"showHijri":true,"carReminders":true,"oilReminder":true,"accent":4283403519}'
prefs "$MODERN" "$FAVS" "$BOOT" "$SEEN$REMIND"
start
sleep 18
shot 40-modern-shortcuts

# 41: the favourites panel from the button on the left edge
adb shell input tap 30 360
sleep 2
shot 41-modern-favourites
adb shell input keyevent KEYCODE_BACK
sleep 1

# 42: modern, the Today panel beside the clock
prefs "${MODERN%\}},\"modernFill\":\"TODAY\"}" "$FAVS" "$BOOT" "$SEEN$REMIND"
start
sleep 16
shot 42-modern-today

# 43: moving: the speed takes the middle; driving mode keeps two cards
prefs "${MODERN%\}},\"drivingMode\":true,\"speedAlert\":true,\"speedLimit\":120}" "$FAVS" "$BOOT" "$SEEN"
start
sleep 12
for i in 1 2 3 4 5 6 7 8; do adb emu geo fix 49.60$i 25.3833 150 8 47; sleep 1; done   # about 87 km/h
shot 43-modern-driving
park
sleep 2

# 44: modern in Arabic, favourites button on the right
adb shell cmd locale set-app-locales $PKG --locales ar || true
prefs "${MODERN%\}},\"favSide\":\"RIGHT\"}" "$FAVS" "$BOOT" "$SEEN$REMIND"
start
sleep 16
shot 44-modern-arabic
adb shell input tap 1890 360
sleep 2
shot 45-modern-arabic-favourites
adb shell input keyevent KEYCODE_BACK
adb shell cmd locale set-app-locales $PKG --locales en || true

# 46: custom home, the starting layout
CUSTOMH='{"homeStyle":"CUSTOM","favCount":9,"showHijri":true,"accent":4283403519}'
prefs "$CUSTOMH" "$FAVS" "$BOOT" "$SEEN"
start
sleep 16
shot 46-custom-home

# 47–49: press and hold to edit; select the clock; the widget library
adb shell input swipe 960 600 960 600 1500
sleep 2
shot 47-custom-edit
adb shell input tap 840 224
sleep 1
shot 48-custom-edit-selected
adb shell input tap 144 630
sleep 2
shot 49-custom-library
adb shell input keyevent KEYCODE_BACK
sleep 1
adb shell input keyevent KEYCODE_BACK
sleep 1

# 50: custom in Arabic
adb shell cmd locale set-app-locales $PKG --locales ar || true
prefs "$CUSTOMH" "$FAVS" "$BOOT" "$SEEN"
start
sleep 16
shot 50-custom-arabic
adb shell cmd locale set-app-locales $PKG --locales en || true

# 51–52: Settings → Home screen: the three styles and the modern options
prefs "$MODERN" "$FAVS" "$BOOT" "$SEEN"
start
sleep 10
adb shell input tap 1848 69
sleep 2
shot 51-settings-home-style
adb shell input swipe 1100 600 1100 200 300
sleep 1
shot 52-settings-modern-options
adb shell input keyevent KEYCODE_BACK

adb shell wm size 1280x720
adb shell wm density 160
