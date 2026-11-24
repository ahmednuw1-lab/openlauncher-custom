# Screenshots of what is new in CarHome 1.4.0: the dashboard home (map, cards, next turn),
# the map and next-turn widgets on the custom home, and the strip beside another app.
# Sourced by screenshots.sh after its setup (permissions, the prefs/start/drive/park helpers).
# Taken on a wide, low car screen like the AI box (1920x720 at 240 dpi = 1280x480 dp).
# The map needs the emulator's internet (tiles come from tile.openstreetmap.org).

adb shell wm size 1920x720
adb shell wm density 240

VC=$(grep -oP 'versionCode\s*=\s*\K[0-9]+' app/build.gradle.kts)
SEEN="<int name=\"last_version_code\" value=\"$VC\" />"
DASH='{"homeStyle":"DASHBOARD","favCount":9,"accent":4283403519,"speedCameras":true}'
DASHDRIVE='{"homeStyle":"DASHBOARD","favCount":9,"accent":4283403519,"drivingMode":true,"speedAlert":true,"speedLimit":120}'
DASHLIGHT='{"homeStyle":"DASHBOARD","favCount":9,"accent":4283403519,"mapLook":"LIGHT","dashMapSide":"RIGHT","dashMapWidth":70,"dashCards":["SPEED","MUSIC","WEATHER"]}'
CUSTH='{"homeStyle":"CUSTOM","favCount":9,"accent":4283403519}'
STRIP='{"homeStyle":"DASHBOARD","favCount":9,"accent":4283403519,"mapsStrip":true,"backgroundTracking":true}'
# 8x4 grid: the map at 4x4 on the left, the next turn 3x1, speed and music, places
ML='<string name="custom_layout">{"v":1,"grid":"NORMAL","pages":1,"widgets":[{"id":"m","type":"MAP","p":0,"c":0,"r":0,"w":4,"h":4,"style":"GLASS"},{"id":"n","type":"NEXT_TURN","p":0,"c":4,"r":0,"w":3,"h":1,"style":"GLASS"},{"id":"k","type":"CLOCK","p":0,"c":7,"r":0,"w":1,"h":1,"style":"CLEAR"},{"id":"s","type":"SPEED","p":0,"c":4,"r":1,"w":2,"h":2,"style":"GLASS"},{"id":"u","type":"MUSIC","p":0,"c":6,"r":1,"w":2,"h":2,"style":"TINTED"},{"id":"h","type":"SHORTCUT","p":0,"c":4,"r":3,"w":2,"h":1,"style":"GLASS","value":"place:home1"},{"id":"a","type":"ALL_APPS","p":0,"c":7,"r":3,"w":1,"h":1,"style":"GLASS"}]}</string>'

# A turn from "Google Maps" (test builds take command-line notifications as Google Maps')
nav_on() { adb shell "cmd notification post -t '300 m' -I 'res:android:drawable/ic_menu_directions' carhome-nav 'Turn right onto King Fahd Rd'" || true; sleep 3; }
# Stand still at the place the other screenshots use, long enough for the map to load
here() { for i in 1 2 3 4 5 6; do adb emu geo fix 49.5920 25.3833 150 8 0; sleep 3; done; }

# 75–76: the dashboard while parked, in English and Arabic
prefs "$DASH" "$APPS7" "$BOOT" "$SEEN"
start
here
sleep 8
shot 75-dash-parked
adb shell cmd locale set-app-locales $PKG --locales ar || true
sleep 3
here
shot 76-dash-parked-arabic
adb shell cmd locale set-app-locales $PKG --locales en || true

# 77: driving (130 km/h): the map further out, driving mode with the two biggest cards
prefs "$DASHDRIVE" "$APPS7" "$BOOT" "$SEEN"
start
here
drive 14
shot 77-dash-driving
park

# 78: Google Maps navigating: the next turn on top of the map
prefs "$DASH" "$APPS7" "$BOOT" "$SEEN"
start
here
nav_on
sleep 2
shot 78-dash-next-turn

# 79: light map on the right, 70% wide, three cards
prefs "$DASHLIGHT" "$APPS7" "$BOOT" "$SEEN"
start
here
sleep 6
shot 79-dash-light-right

# 80: the custom home with the map and next-turn widgets (Arabic)
prefs "$CUSTH" "$APPS7" "$BOOT" "$SEEN$ML"
adb shell cmd locale set-app-locales $PKG --locales ar || true
start
here
nav_on
sleep 4
shot 80-custom-map-widgets-arabic
adb shell cmd locale set-app-locales $PKG --locales en || true

# 81: the strip beside another app while "Google Maps" navigates
prefs "$STRIP" "$APPS7" "$BOOT" "$SEEN"
start
here
nav_on
OTHER=$(echo "$ALL" | grep -m1 -E 'maps' || echo "$ALL" | grep -m1 -E 'settings' || echo "com.android.settings")
adb shell monkey -p "$OTHER" -c android.intent.category.LAUNCHER 1 || true
sleep 8
shot 81-strip-over-other-app
adb shell input keyevent KEYCODE_HOME
sleep 2

adb shell wm size 1280x720
adb shell wm density 160
