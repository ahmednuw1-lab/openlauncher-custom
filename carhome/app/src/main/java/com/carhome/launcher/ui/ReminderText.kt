package com.carhome.launcher.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.carhome.launcher.CarReminder
import com.carhome.launcher.DueStatus
import com.carhome.launcher.R
import com.carhome.launcher.ReminderType
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

fun reminderNameRes(t: ReminderType): Int = when (t) {
    ReminderType.REGISTRATION -> R.string.rem_registration
    ReminderType.INSURANCE -> R.string.rem_insurance
    ReminderType.INSPECTION -> R.string.rem_inspection
    ReminderType.LICENCE -> R.string.rem_licence
    ReminderType.TYRES -> R.string.rem_tyres
    ReminderType.BATTERY -> R.string.rem_battery
    ReminderType.SERVICE -> R.string.rem_service
    ReminderType.CUSTOM -> R.string.rem_custom
}

fun reminderIcon(t: ReminderType): ImageVector = when (t) {
    ReminderType.REGISTRATION, ReminderType.LICENCE -> Icons.Rounded.Description
    ReminderType.INSURANCE, ReminderType.INSPECTION -> Icons.Rounded.VerifiedUser
    ReminderType.TYRES -> Icons.Rounded.DirectionsCar
    ReminderType.BATTERY -> Icons.Rounded.BatteryChargingFull
    ReminderType.SERVICE -> Icons.Rounded.Build
    ReminderType.CUSTOM -> Icons.Rounded.Event
}

@Composable
fun reminderName(r: CarReminder): String =
    if (r.type == ReminderType.CUSTOM && r.name.isNotBlank()) r.name else stringResource(reminderNameRes(r.type))

/** "Insurance: 12 days left", "Tyres: 300 km left", "Inspection: 3 days late"… */
@Composable
fun reminderDueText(st: DueStatus, useKmh: Boolean): String {
    val name = reminderName(st.reminder)
    val days = st.daysLeft
    val km = st.kmLeft
    // Show the closer of the two when a reminder has both
    val useKm = km != null && (days == null || km / 50.0 < days)
    if (useKm && km != null) {
        val unit = stringResource(if (useKmh) R.string.km else R.string.mi)
        val v = abs(if (useKmh) km else km / 1.609344).roundToLong()
        val n = String.format(Locale.US, "%,d", v)
        return if (km < 0) stringResource(R.string.reminder_km_over, name, n, unit)
        else stringResource(R.string.reminder_km_left, name, n, unit)
    }
    val d = days ?: 0
    return when {
        d == 0 -> stringResource(R.string.reminder_today, name)
        d < 0 -> pluralStringResource(R.plurals.reminder_days_late, -d, name, -d)
        else -> pluralStringResource(R.plurals.reminder_days_left, d, name, d)
    }
}
