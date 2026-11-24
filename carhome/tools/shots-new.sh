# Screenshots of what is new in CarHome 1.3.3: the internet sign in each of its states
# (three sizes of the internet widget on the custom home, then the modern top bar).
# Sourced by screenshots.sh after its setup (permissions, the prefs/start/drive/park helpers).
# Taken on a wide, low car screen like the AI box (1920x720 at 240 dpi = 1280x480 dp).
# The emulator has two networks: Wi-Fi "AndroidWifi" and mobile data, both on at the start.

adb shell wm size 1920x720
adb shell wm density 240

VC=$(grep -oP 'versionCode\s*=\s*\K[0-9]+' app/build.gradle.kts)
SEEN="<int name=\"last_version_code\" value=\"$VC\" />"
NETH='{"homeStyle":"CUSTOM","favCount":9,"accent":4283403519}'
# 8x4 grid: the internet widget at 2x2, 2x1 and 1x1, with a clock, the date and the speed
NL='<string name="custom_layout">{"v":1,"grid":"NORMAL","pages":1,"widgets":[{"id":"a","type":"INTERNET","p":0,"c":0,"r":0,"w":2,"h":2,"style":"GLASS"},{"id":"b","type":"CLOCK","p":0,"c":2,"r":0,"w":4,"h":2,"style":"CLEAR"},{"id":"c","type":"INTERNET","p":0,"c":6,"r":0,"w":2,"h":1,"style":"SOLID"},{"id":"d","type":"INTERNET","p":0,"c":6,"r":1,"w":1,"h":1,"style":"GLASS"},{"id":"e","type":"DATE","p":0,"c":0,"r":2,"w":4,"h":2,"style":"CLEAR"},{"id":"f","type":"SPEED","p":0,"c":4,"r":2,"w":4,"h":2,"style":"GLASS"}]}</string>'

# A fresh start, so the sign checks the internet at once
net_shot() { prefs "$NETH" "$APPS7" "$BOOT" "$SEEN$NL"; start; sleep 14; shot "$1"; }
wifi_forget() {
  for id in $(adb shell cmd wifi list-networks | tr -d '\r' | awk 'NR>1 && $1 ~ /^[0-9]+$/ {print $1}'); do
    adb shell cmd wifi forget-network "$id" || true
  done
}
net_log() { echo "== $1"; adb shell dumpsys connectivity | grep -E "^ *NetworkAgentInfo|Active default network" | head -5 || true; }

# 67: Wi-Fi with internet — green
net_log wifi
net_shot 67-net-connected

# 68: the phone's hotspot (a Wi-Fi Android marks as metered) — blue
wifi_forget
adb shell cmd wifi connect-network AndroidWifi open -m || true
sleep 10
net_log hotspot
net_shot 68-net-hotspot

# 69–70: on the hotspot, but the internet doesn't answer (plain HTTP blocked) — red. Android may
# still call this network "validated" (it checks over HTTPS too): the 1.3.2 bug showed green here.
adb root || true
sleep 3
adb wait-for-device
adb shell iptables -I OUTPUT -p tcp --dport 80 -j REJECT || true
adb shell ip6tables -I OUTPUT -p tcp --dport 80 -j REJECT || true
net_log no-internet
net_shot 69-net-no-internet
adb shell cmd locale set-app-locales $PKG --locales ar || true
net_shot 70-net-no-internet-arabic
adb shell cmd locale set-app-locales $PKG --locales en || true
adb shell iptables -D OUTPUT -p tcp --dport 80 -j REJECT || true
adb shell ip6tables -D OUTPUT -p tcp --dport 80 -j REJECT || true

# 71: Wi-Fi off, the box's own mobile data — blue
adb shell svc wifi disable
sleep 10
net_log cellular
net_shot 71-net-cellular

# 72: Wi-Fi off and no mobile data — grey
adb shell svc data disable
sleep 6
net_log off
net_shot 72-net-wifi-off

# 73: Wi-Fi on but no network joined — grey (Arabic)
wifi_forget
adb shell svc wifi enable
sleep 10
net_log not-connected
adb shell cmd locale set-app-locales $PKG --locales ar || true
net_shot 73-net-not-connected-arabic
adb shell cmd locale set-app-locales $PKG --locales en || true

# 74: back on the Wi-Fi — green, on the modern home's top bar
adb shell cmd wifi connect-network AndroidWifi open || true
adb shell svc data enable
sleep 10
net_log back
MODH='{"homeStyle":"MODERN","favCount":9,"accent":4283403519}'
prefs "$MODH" "$APPS7" "$BOOT" "$SEEN"
start
for i in 1 2 3 4 5; do adb emu geo fix 49.5920 25.3833 150 8 0; sleep 3; done
shot 74-net-modern-connected

adb shell wm size 1280x720
adb shell wm density 160
