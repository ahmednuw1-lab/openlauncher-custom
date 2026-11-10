# Screenshots of what is new in CarHome 1.2.11. Sourced by screenshots.sh after
# its setup (emulator at 1280x720 mdpi, permissions, the prefs/start/drive/park helpers).

MUSICAPP=$(echo "$ALL" | head -1)

# 20–22: a wide, low car screen like the AI box (1920x720 at 240 dpi = 1280x480 dp):
# the half-circle speed gauge, and the weather strip with as many hours as fit
adb shell wm size 1920x720
adb shell wm density 240
LOW='{"tileCount":8,"cards":["CLOCK","SPEED","MUSIC","TRIP"],"topBar":"WEATHER","speedAlert":true,"speedLimit":120}'
prefs "$LOW" "$APPS7" "$BOOT" '<int name="last_version_code" value="14" />'
start
sleep 4
adb emu geo fix 49.5886 25.3833
sleep 20
drive 4
for i in 1 2 3 4 5 6; do adb emu geo fix 49.60$i 25.3833 150 8 43; sleep 1; done   # about 80 km/h
shot 20-low-screen-gauge
# the arrow at the end of the weather strip: the next hours
adb shell input tap 1635 78
sleep 2
shot 21-low-screen-weather-later-hours
drive 7
shot 22-low-screen-gauge-over-limit
park
sleep 2

# 23: the same low screen in Arabic (the gauge is never mirrored)
adb shell cmd locale set-app-locales $PKG --locales ar || true
prefs "$LOW" "$APPS7" "$BOOT" '<int name="last_version_code" value="14" />'
start
sleep 12
for i in 1 2 3 4 5 6; do adb emu geo fix 49.61$i 25.3833 150 8 32; sleep 1; done   # about 60 km/h
shot 23-low-screen-arabic
park
adb shell cmd locale set-app-locales $PKG --locales en || true

# Back to the normal 1280x720 screen
adb shell wm size 1280x720
adb shell wm density 160

# 24: "Ice" theme — ice-blue accent, frosted cards, extra-round corners, round icons, date in the top bar
ICE='{"tileCount":8,"cards":["CLOCK","SPEED","WEATHER"],"topBar":"DATE","accent":4286436348,"cardFill":"FROSTED","corners":"EXTRA","iconShape":"CIRCLE","showHijri":true}'
prefs "$ICE" "$APPS7" "$BOOT" '<int name="last_version_code" value="14" />'
start
sleep 16
for i in 1 2 3 4 5; do adb emu geo fix 49.62$i 25.3833 150 8 49; sleep 1; done   # about 90 km/h
shot 24-theme-ice-date-bar
park

# 25: "Carbon" — pearl accent, solid cards, square corners, large icons, trip in the top bar
CARBON='{"tileCount":6,"cards":["CLOCK","SPEED","PRAYER"],"topBar":"TRIP","accent":4293257195,"cardFill":"SOLID","corners":"SQUARE","iconSize":"LARGE","iconShape":"ROUNDED"}'
prefs "$CARBON" "$APPS7" "$BOOT" '<int name="last_version_code" value="14" />'
start
sleep 16
shot 25-theme-carbon-trip-bar

# 26: high contrast over the living sky, lime accent, music in the top bar
HC='{"tileCount":8,"cards":["CLOCK","SPEED","MUSIC"],"topBar":"MUSIC","accent":4288931381,"background":"SKY","highContrast":true,"showPrayerChip":true}'
prefs "$HC" "$APPS7" "$BOOT" '<int name="last_version_code" value="14" />'
start
sleep 16
shot 26-high-contrast-music-bar

# 27: clear cards with a narrow weather strip beside the prayer chip (fewer hours fit)
NARROW='{"tileCount":8,"cards":["CLOCK","SPEED","MUSIC"],"topBar":"WEATHER","showPrayerChip":true,"infoWidth":"WIDE","cardFill":"CLEAR","accent":4294933061}'
prefs "$NARROW" "$APPS7" "$BOOT" '<int name="last_version_code" value="14" />'
start
sleep 16
shot 27-narrow-weather-strip

# 28–30: Settings → Display with a custom colour (the two sliders are open)
CUSTOMC='{"tileCount":8,"topBar":"WEATHER","accent":4284534015}'
prefs "$CUSTOMC" "$APPS7" "$BOOT" '<int name="last_version_code" value="14" />'
start
sleep 8
adb shell input tap 1232 48
sleep 2
adb shell input tap 145 430
sleep 2
shot 28-settings-display-themes-colours
adb shell input swipe 800 620 800 200 300
sleep 1
shot 29-settings-display-custom-colour
adb shell input swipe 800 620 800 200 300
sleep 1
shot 30-settings-display-card-style

# 31–32: Settings → Home screen: app icons and the top bar choice
adb shell input tap 145 130
sleep 2
shot 31-settings-home-icons
adb shell input swipe 800 620 800 330 300
sleep 1
shot 32-settings-home-cards
adb shell input swipe 800 620 800 330 300
sleep 1
shot 33-settings-home-top-bar
adb shell input keyevent KEYCODE_BACK
