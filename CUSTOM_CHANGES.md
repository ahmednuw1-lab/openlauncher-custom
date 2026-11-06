# Open Launcher — Custom build 0.0.5-custom.1

Based on upstream **v0.0.5** (dw2lam/openlauncher), reviewed and patched for
use on an Android CarPlay "AI box" (Qualcomm, landscape car screen, connected
through a phone hotspot, no FM radio / CAN bus).

Installs **next to** the official app (package `com.openlauncher.app.custom`,
label "Open Launcher Custom"), so nothing has to be uninstalled and you can
switch back at any time in *Settings → Apps → Default apps → Home app*.

## Bug fixes

| # | Problem | Fix | File |
|---|---------|-----|------|
| 1 | **Back button closed the launcher**; Android restarted it, so the screen flashed black and reloaded | Back now returns to the home page; on the home page it does nothing | `MainActivity.kt` |
| 2 | Pressing **Home** while in Apps/Settings stayed on that page | Home now jumps back to the home page and cancels any picker | `MainActivity.kt` |
| 3 | **Trip tracker reset** (and other widget state was lost) whenever you opened Apps or Settings | Each page's saved state is kept with a `SaveableStateHolder` | `MainActivity.kt` |
| 4 | **Moving a widget** could reset it or hand its state to another widget | Widgets are keyed by id in the grid | `HomeScreen.kt` |
| 5 | **Newly installed apps never appeared** in the app library until the launcher was killed | Listens for install / update / uninstall and refreshes the list | `LauncherViewModel.kt` |
| 6 | If loading the app list threw, the library spun forever and never reloaded | Loading flag always cleared (`try/finally`), queued reloads are honoured | `LauncherViewModel.kt` |
| 7 | Opening a removed / disabled app (shortcut, CarPlay button) could **crash** the launcher | Launch is guarded; shows "Can't open this app" instead | `LauncherViewModel.kt` |
| 8 | **Weather stayed empty for 30 min** if the first try failed (typical after boot: GPS before the hotspot) | Retries every 2 min after a failure and immediately when the network comes up | `LauncherViewModel.kt` |
| 9 | Wi-Fi / data icons only updated when you left and came back to the launcher | Live network callback | `LauncherViewModel.kt` |
| 10 | **Widget layout reset itself** whenever the right-hand column was empty (bad "old layout" migration check) | Migration now runs only for layouts saved by the old 2×2 build (layout version key) | `SettingsRepository.kt` |
| 11 | Damaged/old saved settings could still crash or make widgets vanish (Gson leaves missing fields null/0) | Shortcuts, widgets and sound pads are repaired on load; scales clamped to sane ranges | `SettingsRepository.kt` |
| 12 | "Notification Access" button **crashed** on boxes without that Settings screen | Falls back to the per-app page, then App info | `SettingsScreen.kt` |
| 13 | "Check for Updates" **crashed** when no browser is installed | Guarded, shows a message | `SettingsScreen.kt` |
| 14 | Removing a shortcut while dragging could crash (stale index) | Index checks | `LauncherViewModel.kt`, `MainActivity.kt` |

## Improvements for the AI box

* **App library "Apps" tab now shows preinstalled apps** (YouTube, Netflix,
  Maps, the CarPlay app…). On AI boxes these are *system* apps, so the old
  "Installed" tab hid them. "System" now lists only components without a
  launcher icon.
* **Vitals widget is honest:** Android 8+ blocks CPU usage for normal apps, and
  the old code then showed an invented sine-wave "load" and a fixed 38 °C.
  It now shows **N/A** when a value can't be read.
* **Now Playing reacts faster:** listens for media-session changes directly,
  not only for notifications.
* The launcher no longer restarts when the car display changes mode
  (day/night `uiMode`, screen layout, keyboard).
* Removed the unused **BODY_SENSORS** permission (heart-rate class sensors).
* GitHub Actions workflow (`.github/workflows/build-apk.yml`) builds the APK
  in the cloud — no Android Studio needed.

## Known limitations (not changed)

* The trip tracker keeps its totals while you visit Apps/Settings now, but it
  only counts while the home page is on screen. Moving it into the ViewModel
  would let it run in the background — a good next step.
* Radio features target szchoiceway head units and do nothing on an AI box.
* This build was reviewed and syntax-checked but **not compiled** in the
  environment it was edited in (Android build servers were unreachable).
  Build it once with the workflow or Android Studio; report any compile
  error and it can be fixed quickly.
