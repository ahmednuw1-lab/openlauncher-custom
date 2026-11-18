# Screenshots of what is new in CarHome 1.3.2: the speed widget at every size on the
# custom home screen (the half-circle gauge must fit its box, with the text beside or under it).
# Sourced by screenshots.sh after its setup (permissions, the prefs/start/drive/park helpers).
# Taken on a wide, low car screen like the AI box (1920x720 at 240 dpi = 1280x480 dp).

adb shell wm size 1920x720
adb shell wm density 240

VC=$(grep -oP 'versionCode\s*=\s*\K[0-9]+' app/build.gradle.kts)
SEEN="<int name=\"last_version_code\" value=\"$VC\" />"
SPEEDH='{"homeStyle":"CUSTOM","favCount":9,"accent":4283403519,"speedAlert":true,"speedLimit":120}'

# 60–61: 8x4 grid, speed boxes of many sizes (2x2, 3x1, 2x1, 1x1, 3x2, 4x1, 2x2, 2x1), while moving
SL8='<string name="custom_layout">{"v":1,"grid":"NORMAL","pages":1,"widgets":[{"id":"a","type":"SPEED","p":0,"c":0,"r":0,"w":2,"h":2,"style":"GLASS"},{"id":"b","type":"SPEED","p":0,"c":2,"r":0,"w":3,"h":1,"style":"GLASS"},{"id":"c","type":"SPEED","p":0,"c":2,"r":1,"w":2,"h":1,"style":"SOLID"},{"id":"d","type":"SPEED","p":0,"c":4,"r":1,"w":1,"h":1,"style":"GLASS"},{"id":"e","type":"SPEED","p":0,"c":5,"r":0,"w":3,"h":2,"style":"CLEAR"},{"id":"f","type":"SPEED","p":0,"c":0,"r":2,"w":4,"h":1,"style":"GLASS"},{"id":"g","type":"SPEED","p":0,"c":4,"r":2,"w":2,"h":2,"style":"TINTED"},{"id":"h","type":"CLOCK","p":0,"c":6,"r":2,"w":2,"h":2,"style":"CLEAR"},{"id":"i","type":"SPEED","p":0,"c":0,"r":3,"w":2,"h":1,"style":"GLASS"},{"id":"j","type":"DATE","p":0,"c":2,"r":3,"w":2,"h":1,"style":"CLEAR"}]}</string>'
prefs "$SPEEDH" "$APPS7" "$BOOT" "$SEEN$SL8"
start
sleep 12
for i in 1 2 3 4 5 6 7 8; do adb emu geo fix 49.60$i 25.3833 150 8 46; sleep 1; done   # about 85 km/h
shot 60-speed-sizes-8x4
for i in 1 2 3 4 5 6; do adb emu geo fix 49.61$i 25.3833 150 8 72; sleep 1; done      # about 133 km/h: over the limit
shot 61-speed-sizes-over-limit
park

# 62: the same in Arabic, parked (no speed yet: the dash)
adb shell cmd locale set-app-locales $PKG --locales ar || true
prefs "$SPEEDH" "$APPS7" "$BOOT" "$SEEN$SL8"
start
sleep 12
for i in 1 2 3 4 5 6; do adb emu geo fix 49.62$i 25.3833 150 8 33; sleep 1; done      # about 60 km/h
shot 62-speed-sizes-arabic
park
adb shell cmd locale set-app-locales $PKG --locales en || true

# 63: 6x3 grid (bigger cells) and 64: 10x5 grid (smaller cells)
SL6='<string name="custom_layout">{"v":1,"grid":"SMALL","pages":1,"widgets":[{"id":"a","type":"SPEED","p":0,"c":0,"r":0,"w":1,"h":1,"style":"GLASS"},{"id":"b","type":"SPEED","p":0,"c":1,"r":0,"w":2,"h":1,"style":"GLASS"},{"id":"c","type":"SPEED","p":0,"c":3,"r":0,"w":3,"h":2,"style":"GLASS"},{"id":"d","type":"SPEED","p":0,"c":0,"r":1,"w":3,"h":2,"style":"SOLID"},{"id":"e","type":"CLOCK","p":0,"c":3,"r":2,"w":3,"h":1,"style":"CLEAR"}]}</string>'
prefs "$SPEEDH" "$APPS7" "$BOOT" "$SEEN$SL6"
start
sleep 12
for i in 1 2 3 4 5 6; do adb emu geo fix 49.63$i 25.3833 150 8 46; sleep 1; done
shot 63-speed-sizes-6x3
SL10='<string name="custom_layout">{"v":1,"grid":"LARGE","pages":1,"widgets":[{"id":"a","type":"SPEED","p":0,"c":0,"r":0,"w":2,"h":2,"style":"GLASS"},{"id":"b","type":"SPEED","p":0,"c":2,"r":0,"w":3,"h":1,"style":"GLASS"},{"id":"c","type":"SPEED","p":0,"c":5,"r":0,"w":2,"h":1,"style":"GLASS"},{"id":"d","type":"SPEED","p":0,"c":7,"r":0,"w":3,"h":3,"style":"GLASS"},{"id":"e","type":"SPEED","p":0,"c":2,"r":1,"w":4,"h":2,"style":"SOLID"},{"id":"f","type":"SPEED","p":0,"c":0,"r":2,"w":2,"h":1,"style":"GLASS"},{"id":"g","type":"CLOCK","p":0,"c":0,"r":3,"w":4,"h":2,"style":"CLEAR"},{"id":"h","type":"SPEED","p":0,"c":4,"r":3,"w":3,"h":2,"style":"TINTED"}]}</string>'
prefs "$SPEEDH" "$APPS7" "$BOOT" "$SEEN$SL10"
start
sleep 12
for i in 1 2 3 4 5 6; do adb emu geo fix 49.64$i 25.3833 150 8 46; sleep 1; done
shot 64-speed-sizes-10x5
park

# 65–66: classic home, the internet sign beside Settings (English and Arabic)
CLASSICH='{"homeStyle":"CLASSIC","tileCount":8,"cards":["CLOCK","SPEED","MUSIC","TRIP"],"topBar":"WEATHER","showPrayerChip":true,"showHijri":true,"accent":4283403519}'
prefs "$CLASSICH" "$APPS7" "$BOOT" "$SEEN"
start
for i in 1 2 3 4 5 6; do adb emu geo fix 49.5920 25.3833 150 8 0; sleep 3; done
shot 65-classic-internet-sign
adb shell cmd locale set-app-locales $PKG --locales ar || true
prefs "$CLASSICH" "$APPS7" "$BOOT" "$SEEN"
start
for i in 1 2 3 4 5 6; do adb emu geo fix 49.5920 25.3833 150 8 0; sleep 3; done
shot 66-classic-internet-sign-arabic
adb shell cmd locale set-app-locales $PKG --locales en || true

adb shell wm size 1280x720
adb shell wm density 160
