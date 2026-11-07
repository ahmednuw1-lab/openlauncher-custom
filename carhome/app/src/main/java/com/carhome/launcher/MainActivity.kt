package com.carhome.launcher

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carhome.launcher.ui.AppBackground
import com.carhome.launcher.ui.AppsScreen
import com.carhome.launcher.ui.CarHomeTheme
import com.carhome.launcher.ui.HomeActions
import com.carhome.launcher.ui.HomeScreen
import com.carhome.launcher.ui.HomeState
import com.carhome.launcher.ui.NightDim
import com.carhome.launcher.ui.ParkedOverlay
import com.carhome.launcher.ui.SettingsActions
import com.carhome.launcher.ui.SettingsScreen
import com.carhome.launcher.ui.SettingsState
import com.carhome.launcher.ui.WelcomeOverlay
import com.carhome.launcher.ui.WhatsNewOverlay
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()
    private val core get() = CarHomeApp.core(this)

    /** Bumped on resume and after permission results, so permission states re-read. */
    private val resumeTick = mutableIntStateOf(0)

    // Language chosen in CarHome (instead of the box's) applies from here
    private var appliedLanguage: AppLanguage? = null

    override fun attachBaseContext(newBase: Context) {
        val lang = runCatching { CarHomeApp.core(newBase).settings.value.language }.getOrDefault(AppLanguage.SYSTEM)
        appliedLanguage = lang
        // The chosen (or system) language, always with Western digits (123)
        val base = lang.tag?.let { Locale.forLanguageTag(it) }
            ?: newBase.resources.configuration.locales[0] ?: Locale.getDefault()
        val loc = Digits.latin(base)
        Locale.setDefault(loc)
        val cfg = Configuration(newBase.resources.configuration)
        cfg.setLocale(loc)
        cfg.setLayoutDirection(loc)
        super.attachBaseContext(newBase.createConfigurationContext(cfg))
    }

    private val locationPerms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.engine.start()
        DriveService.sync(this)
        vm.refreshWeatherSoon()
        resumeTick.intValue++
    }

    private val simplePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        core.obd.sync()
        resumeTick.intValue++
    }

    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.setPhoto(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        // A launcher must never close on Back: step out of a page, otherwise ignore
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val n = vm.nav.value
                when {
                    n.pickFor == PickFor.AUTO_APP -> vm.openSettings()
                    n.page != Page.HOME -> vm.goHome()
                }
            }
        })

        if (savedInstanceState == null && !vm.engine.hasFine() && !core.prefs.askedLocation) {
            core.prefs.askedLocation = true
            locationPermission.launch(locationPerms)
        }

        setContent {
            val s by vm.settings.collectAsStateWithLifecycle()
            val tiles by vm.tiles.collectAsStateWithLifecycle()
            val apps by vm.apps.collectAsStateWithLifecycle()
            val places by vm.places.collectAsStateWithLifecycle()
            val nav by vm.nav.collectAsStateWithLifecycle()
            val settingsPage by vm.settingsPage.collectAsStateWithLifecycle()
            val weather by vm.weather.collectAsStateWithLifecycle()
            val dust by vm.dustInHours.collectAsStateWithLifecycle()
            val prayer by vm.prayer.collectAsStateWithLifecycle()
            val sun by vm.sun.collectAsStateWithLifecycle()
            val notices by vm.notices.collectAsStateWithLifecycle()
            val smartPick by vm.smartPick.collectAsStateWithLifecycle()
            val parkedOnly by vm.parkedOnlyApps.collectAsStateWithLifecycle()
            val songAccent by vm.songAccent.collectAsStateWithLifecycle()
            val photo by vm.photo.collectAsStateWithLifecycle()
            val night by vm.night.collectAsStateWithLifecycle()
            val parked by vm.parkedScreen.collectAsStateWithLifecycle()
            val welcome by vm.welcome.collectAsStateWithLifecycle()
            val whatsNew by vm.whatsNew.collectAsStateWithLifecycle()
            val drivingNow by vm.engine.moving.collectAsStateWithLifecycle()
            val driving by vm.drivingMode.collectAsStateWithLifecycle()
            val nowPlaying by vm.nowPlaying.collectAsStateWithLifecycle()
            val voiceStatus by vm.voiceStatus.collectAsStateWithLifecycle()
            val speedMps by vm.engine.speedMps.collectAsStateWithLifecycle()
            val gpsState by vm.engine.gpsState.collectAsStateWithLifecycle()
            val satellites by vm.engine.satellites.collectAsStateWithLifecycle()
            val speedFromCar by vm.engine.speedFromCar.collectAsStateWithLifecycle()
            val speedStale by vm.engine.speedStale.collectAsStateWithLifecycle()
            val speedApprox by vm.engine.speedApprox.collectAsStateWithLifecycle()
            val gpsDiag by vm.engine.diag.collectAsStateWithLifecycle()
            val updateState by core.updater.state.collectAsStateWithLifecycle()
            val overspeed by vm.engine.overspeed.collectAsStateWithLifecycle()
            val limit by vm.engine.limit.collectAsStateWithLifecycle()
            val trip by vm.engine.trip.collectAsStateWithLifecycle()
            val lastTrip by vm.engine.lastTrip.collectAsStateWithLifecycle()
            val odometer by vm.engine.odometerKm.collectAsStateWithLifecycle()
            val oilAt by vm.engine.oilChangedAtKm.collectAsStateWithLifecycle()
            val obdStatus by core.obd.status.collectAsStateWithLifecycle()
            val obdData by core.obd.data.collectAsStateWithLifecycle()

            // A language change needs the screen rebuilt with the new resources
            LaunchedEffect(s.language) {
                if (s.language != appliedLanguage) recreate()
            }

            val tick = resumeTick.intValue
            val hasMusic = remember(tick) { MediaService.hasAccess(this) }
            val hasLocation = remember(tick) { vm.engine.hasAnyLocation() }
            val hasFine = remember(tick) { vm.engine.hasFine() }
            val canOverlay = remember(tick) { Settings.canDrawOverlays(this) }
            val hasNotif = remember(tick) { Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS) }
            val canWrite = remember(tick) { vm.canWriteSystem() }
            val hasBt = remember(tick) { core.obd.hasPermission() }
            val paired = remember(tick, s.obd) { if (s.obd) vm.pairedObd() else emptyList() }
            val scores = remember(tick, trip.startedAt, settingsPage) { vm.scores() }
            val appsByPackage = remember(apps) { apps.associateBy { it.packageName } }
            val placesById = remember(places) { places.associateBy { it.id } }

            val baseAccent = Color(s.accent)
            val accent by animateColorAsState(songAccent?.let { Color(it) } ?: baseAccent, tween(900), label = "accent")
            val density = LocalDensity.current

            CarHomeTheme(accent = accent) {
                CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * s.textSize.scale)) {
                    AppBackground(
                        style = s.background,
                        accent = accent,
                        photo = photo,
                        sun = sun,
                        weatherCode = weather?.code,
                        dust = s.dustWarning && dust == 0
                    ) {
                        AnimatedContent(
                            targetState = nav,
                            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.98f)) togetherWith fadeOut() },
                            label = "page"
                        ) { current ->
                            when (current.page) {
                                Page.HOME -> HomeScreen(
                                    HomeState(
                                        settings = s, accent = accent, tiles = tiles, appsByPackage = appsByPackage,
                                        placesById = placesById, smartPick = smartPick?.let { appsByPackage[it] },
                                        weather = weather, dustInHours = dust, hasLocation = hasLocation,
                                        nowPlaying = nowPlaying, hasMusicAccess = hasMusic, speedMps = speedMps,
                                        gpsState = gpsState, satellites = satellites.first, speedFromCar = speedFromCar, speedStale = speedStale, speedApprox = speedApprox,
                                        overspeed = overspeed, limit = limit, prayer = prayer,
                                        trip = trip, lastTrip = lastTrip, obdStatus = obdStatus, obdData = obdData,
                                        driving = driving, notices = notices, parkedOnly = parkedOnly
                                    ),
                                    homeActions
                                )

                                Page.APPS -> AppsScreen(
                                    apps = apps,
                                    accent = accent,
                                    pickerMode = current.pickFor != null,
                                    allowPlaces = current.pickFor == PickFor.TILE && s.placeTiles,
                                    allowSmart = current.pickFor == PickFor.TILE && s.smartTile,
                                    places = places,
                                    parkedOnlyOn = s.parkedOnly,
                                    parkedOnly = parkedOnly,
                                    driving = driving,
                                    onBack = { if (current.pickFor == PickFor.AUTO_APP) vm.openSettings() else vm.goHome() },
                                    onLaunch = { vm.launchApp(it) },
                                    onPick = { vm.picked(it) },
                                    onAddToHome = { pkg ->
                                        if (!vm.addToFirstEmptyTile(pkg)) {
                                            Toast.makeText(this@MainActivity, R.string.no_empty_tile, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onToggleParked = { vm.toggleParkedOnly(it) },
                                    onAddPlace = { n, cur, addr -> addPlace(n, cur, addr) },
                                    onRemovePlace = { vm.removePlace(it) }
                                )

                                Page.SETTINGS -> SettingsScreen(
                                    SettingsState(
                                        settings = s, accent = accent, page = settingsPage, hasMusicAccess = hasMusic,
                                        hasLocation = hasLocation, hasFineLocation = hasFine, canOverlay = canOverlay,
                                        hasNotifications = hasNotif, canWriteSystem = canWrite, hasBluetooth = hasBt,
                                        voiceStatus = voiceStatus, appsByPackage = appsByPackage, places = places,
                                        odometerKm = odometer, oilChangedAtKm = oilAt, obdStatus = obdStatus,
                                        pairedDevices = paired, scores = scores, prayer = prayer, parkedOnly = parkedOnly,
                                        gps = gpsDiag, update = updateState
                                    ),
                                    settingsActions
                                )
                            }
                        }

                        ParkedOverlay(parked, s, weather, prayer, accent) { vm.userTouched() }
                        WelcomeOverlay(welcome, s, weather, prayer, lastTrip, accent) { vm.dismissWelcome() }
                        // Not while driving: it waits until the car stops
                        WhatsNewOverlay(if (drivingNow) null else whatsNew, vm.versionName, accent) { vm.closeWhatsNew() }
                        NightDim(night, s.nightDim)

                        // Pulsing red frame around the whole screen while over the limit. Drawn
                        // last, so night dimming and the parked/welcome screens can't hide it;
                        // the pulse is read only while drawing, so it doesn't rebuild the screen.
                        if (overspeed) {
                            val pulse = rememberInfiniteTransition(label = "alert")
                            val alpha = pulse.animateFloat(
                                initialValue = 0.35f, targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                                label = "alertAlpha"
                            )
                            Box(Modifier.fillMaxSize().drawBehind {
                                val w = 8.dp.toPx()
                                drawRect(
                                    color = Color(0xFFEF4444).copy(alpha = alpha.value),
                                    topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
                                    size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w)
                                )
                            })
                        }
                    }
                }
            }
        }
    }

    private val homeActions by lazy {
        HomeActions(
            openTile = { i, v -> vm.openTile(i, v) },
            changeTile = { vm.pickForTile(it) },
            removeTile = { vm.setTile(it, "") },
            toggleParkedOnly = { vm.toggleParkedOnly(it) },
            openApps = { vm.openApps() },
            openSettings = { vm.openSettingsPage(SettingsPage.HOME_LAYOUT); vm.openSettings() },
            openCarSettings = { vm.openSettingsPage(SettingsPage.TRIP); vm.openSettings() },
            requestLocation = { requestLocation() },
            requestMusicAccess = { openMusicAccess() },
            openMusicApp = { vm.launchApp(it) },
            playPause = { vm.playPause() },
            next = { vm.next() },
            previous = { vm.previous() },
            nearby = { vm.openNearby(it) },
            dismissNotice = { vm.dismissNotice(it) },
            oilChanged = { vm.oilChanged() },
            updateNow = { vm.installUpdate() },
            newTrip = { vm.newTrip() },
            readCodes = { vm.readCodes() }
        )
    }

    private val settingsActions by lazy {
        SettingsActions(
            back = { vm.goHome() },
            openPage = { vm.openSettingsPage(it) },
            update = { vm.updateSettings(it) },
            setDefaultHome = { openHomeSettings() },
            musicAccess = { openMusicAccess() },
            locationAccess = { requestLocation() },
            openLocationSettings = { openLocationSettings() },
            overlayAccess = { openSpecial(Settings.ACTION_MANAGE_OVERLAY_PERMISSION) },
            notificationAccess = {
                if (Build.VERSION.SDK_INT >= 33) simplePermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            writeSystemAccess = { openSpecial(Settings.ACTION_MANAGE_WRITE_SETTINGS) },
            bluetoothAccess = {
                if (Build.VERSION.SDK_INT >= 31) simplePermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                else core.obd.sync()
            },
            testAlert = { vm.testAlert() },
            testChime = { vm.testChime() },
            testVoice = { vm.testVoice() },
            newTrip = { vm.newTrip(); Toast.makeText(this, R.string.trip_started, Toast.LENGTH_SHORT).show() },
            oilChanged = { vm.oilChanged(); Toast.makeText(this, R.string.oil_reset_done, Toast.LENGTH_SHORT).show() },
            adjustOil = { vm.adjustOil(it) },
            pickAutoApp = { vm.pickAutoApp() },
            pickPhoto = { runCatching { photoPicker.launch("image/*") }.onFailure { Toast.makeText(this, R.string.no_file_picker, Toast.LENGTH_LONG).show() } },
            addPlace = { n, cur, addr -> addPlace(n, cur, addr) },
            removePlace = { vm.removePlace(it) },
            toggleParkedOnly = { vm.toggleParkedOnly(it) },
            checkUpdate = { vm.checkForUpdate() },
            installUpdate = { vm.installUpdate() },
            showWhatsNew = { vm.showWhatsNew(); vm.goHome() }
        )
    }

    private fun addPlace(name: String, useCurrent: Boolean, address: String): Boolean {
        val p = vm.addPlace(name, useCurrent, address)
        if (p == null && useCurrent) Toast.makeText(this, R.string.place_no_location, Toast.LENGTH_SHORT).show()
        return p != null
    }

    private fun granted(p: String) = checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    // Pressing Home while CarHome is already showing returns to the home page
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) vm.goHome()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        vm.userTouched()
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
        // Coming back to CarHome counts as a touch: show the tiles, not the parked clock
        vm.userTouched()
        MediaService.requestRefresh(this)
        // Back from "Allow CarHome to install apps": carry on with the update
        if (core.updater.state.value.stage == UpdateStage.NEED_PERMISSION && packageManager.canRequestPackageInstalls()) {
            vm.installUpdate()
        }
        vm.engine.start()
        core.obd.sync()
    }

    override fun onStart() {
        super.onStart()
        core.uiVisible.value = true
        vm.engine.attach("ui")
        DriveService.sync(this)
    }

    override fun onStop() {
        core.uiVisible.value = false
        vm.engine.detach("ui")
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // ── System screens (each with fallbacks: AI boxes often strip Settings) ──

    private fun tryStart(intent: Intent): Boolean =
        runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)

    private fun appDetails() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))

    /** Screens like "Display over other apps", with the app pre-selected where supported. */
    private fun openSpecial(action: String) {
        val opened = tryStart(Intent(action, Uri.parse("package:$packageName"))) || tryStart(Intent(action))
        if (!opened) tryStart(appDetails())
    }

    private fun openLocationSettings() {
        if (!tryStart(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))) tryStart(Intent(Settings.ACTION_SETTINGS))
    }

    private fun requestLocation() {
        // Permission is there but Android's Location switch is off: open that switch
        if (vm.engine.hasFine() && !vm.engine.locationEnabled()) { openLocationSettings(); return }
        if (vm.engine.hasFine()) return
        // After "Don't ask again" the system dialog no longer appears — open app info instead
        if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) && core.prefs.askedLocation) {
            tryStart(appDetails())
        } else {
            core.prefs.askedLocation = true
            locationPermission.launch(locationPerms)
        }
    }

    private fun openMusicAccess() {
        val opened = tryStart(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) ||
            (Build.VERSION.SDK_INT >= 30 && tryStart(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(this, MediaService::class.java).flattenToString()
                )
            ))
        if (!opened) tryStart(appDetails())
    }

    private fun openHomeSettings() {
        val opened = tryStart(Intent(Settings.ACTION_HOME_SETTINGS)) ||
            tryStart(Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"))
        if (!opened) tryStart(Intent(Settings.ACTION_SETTINGS))
    }
}
