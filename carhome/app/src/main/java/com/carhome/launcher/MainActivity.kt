package com.carhome.launcher

import android.Manifest
import android.content.ComponentName
import android.content.Intent
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carhome.launcher.ui.AppsScreen
import com.carhome.launcher.ui.CarHomeTheme
import com.carhome.launcher.ui.GradientBackground
import com.carhome.launcher.ui.HomeScreen
import com.carhome.launcher.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()

    // Bumped on every resume so permission states are re-read after the user
    // comes back from a system Settings screen
    private val resumeTick = mutableIntStateOf(0)

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.startLocation()
        resumeTick.intValue++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        // A launcher must never close on Back: step out of a page, otherwise ignore
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (vm.nav.value.page != Page.HOME) vm.goHome()
            }
        })

        if (savedInstanceState == null && vm.settings.value.showWeather && !vm.hasLocationPermission()) {
            locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val tiles by vm.tiles.collectAsStateWithLifecycle()
            val apps by vm.apps.collectAsStateWithLifecycle()
            val nav by vm.nav.collectAsStateWithLifecycle()
            val weather by vm.weather.collectAsStateWithLifecycle()
            val nowPlaying by vm.nowPlaying.collectAsStateWithLifecycle()

            val tick = resumeTick.intValue
            val hasMusic = remember(tick) { MediaService.hasAccess(this) }
            val hasLocation = remember(tick) { vm.hasLocationPermission() }
            val appsByPackage = remember(apps) { apps.associateBy { it.packageName } }
            val accent = Color(settings.accent)

            CarHomeTheme(accent = accent) {
                GradientBackground(accent = accent) {
                    AnimatedContent(
                        targetState = nav,
                        transitionSpec = {
                            (fadeIn() + scaleIn(initialScale = 0.98f)) togetherWith fadeOut()
                        },
                        label = "page"
                    ) { current ->
                        when (current.page) {
                            Page.HOME -> HomeScreen(
                                settings = settings,
                                accent = accent,
                                tiles = tiles,
                                appsByPackage = appsByPackage,
                                weather = weather,
                                hasLocation = hasLocation,
                                nowPlaying = nowPlaying,
                                hasMusicAccess = hasMusic,
                                onTileClick = { index, pkg ->
                                    if (pkg.isEmpty()) vm.pickAppForTile(index) else vm.launchApp(pkg)
                                },
                                onTileChange = { index -> vm.pickAppForTile(index) },
                                onTileRemove = { index -> vm.setTile(index, "") },
                                onOpenApps = { vm.openApps() },
                                onOpenSettings = { vm.openSettings() },
                                onRequestLocation = { requestLocation() },
                                onRequestMusicAccess = { openMusicAccess() },
                                onOpenMusicApp = { pkg -> vm.launchApp(pkg) },
                                onPlayPause = { vm.playPause() },
                                onNext = { vm.next() },
                                onPrevious = { vm.previous() }
                            )

                            Page.APPS -> AppsScreen(
                                apps = apps,
                                pickerMode = current.pickTile >= 0,
                                accent = accent,
                                onBack = { vm.goHome() },
                                onAppClick = { app ->
                                    if (current.pickTile >= 0) {
                                        vm.setTile(current.pickTile, app.packageName)
                                        vm.goHome()
                                    } else {
                                        vm.launchApp(app.packageName)
                                    }
                                },
                                onAddToHome = { app ->
                                    if (!vm.addToFirstEmptyTile(app.packageName)) {
                                        Toast.makeText(this@MainActivity, R.string.no_empty_tile, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            Page.SETTINGS -> SettingsScreen(
                                settings = settings,
                                accent = accent,
                                hasMusicAccess = hasMusic,
                                hasLocation = hasLocation,
                                onBack = { vm.goHome() },
                                onUpdate = { block -> vm.updateSettings(block) },
                                onSetDefault = { openHomeSettings() },
                                onMusicAccess = { openMusicAccess() },
                                onLocationAccess = { requestLocation() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Pressing Home while CarHome is already showing returns to the home page
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) vm.goHome()
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
        MediaService.requestRefresh()
        if (vm.settings.value.showWeather) vm.startLocation()
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

    private fun appDetails() = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")
    )

    private fun requestLocation() {
        if (vm.hasLocationPermission()) return
        // After "Don't ask again" the system dialog no longer appears — open app info instead
        if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) &&
            getSharedPreferences("carhome", MODE_PRIVATE).getBoolean("asked_location", false)
        ) {
            tryStart(appDetails())
        } else {
            getSharedPreferences("carhome", MODE_PRIVATE).edit().putBoolean("asked_location", true).apply()
            locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
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
