package com.openlauncher.app.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val isSystemApp: Boolean = false,
    // Has an icon in the system app drawer (MAIN/LAUNCHER activity). On AI boxes
    // and head units most real apps (YouTube, Netflix, the CarPlay app) are
    // preinstalled as *system* apps, so "system vs user" alone hides them.
    val isLaunchable: Boolean = true
)
