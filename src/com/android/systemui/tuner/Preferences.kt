/*
 * SPDX-FileCopyrightText: The Proton AOSP Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.tuner

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.UserHandle
import android.provider.Settings
import android.provider.Settings.System.SHOW_BATTERY_PERCENT
import android.text.TextUtils
import androidx.collection.ArraySet
import androidx.collection.arraySetOf

const val PREFS_NAME = "systemui_tuner_preferences"
const val PACKAGE_SYSTEMUI = "com.android.systemui"

fun Context.getDePrefs(): SharedPreferences {
    return createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

fun SharedPreferences.getEnabled(context: Context): Boolean {
    return getBoolean(
        context.getString(R.string.pref_key_enabled),
        context.resources.getBoolean(R.bool.config_default_enabled),
    )
}

fun SharedPreferences.setEnabled(context: Context, value: Boolean) {
    edit().putBoolean(context.getString(R.string.pref_key_enabled), value).apply()
}

fun Context.getSystemUiResources(): Resources? {
    val pm = this.packageManager
    try {
        return pm.getResourcesForApplication(PACKAGE_SYSTEMUI)
    } catch (e: Exception) {
        return null
    }
}

fun Context.getIconHideList(hideListStr: String?): ArraySet<String> {
    val ret: ArraySet<String> = arraySetOf<String>()
    val hideList: List<String> =
        if (hideListStr == null) {
            val resources = this.getSystemUiResources()
            val resourceId =
                resources?.getIdentifier(
                    "config_statusBarIconsToExclude",
                    "array",
                    PACKAGE_SYSTEMUI,
                ) ?: 0
            if (resourceId != 0) {
                resources?.getStringArray(resourceId)?.toList() ?: emptyList<String>()
            }
            emptyList<String>()
        } else {
            hideListStr.split(",").filter { !it.isEmpty() }
        }
    for (slot in hideList) {
        if (!TextUtils.isEmpty(slot)) {
            ret.add(slot)
        }
    }
    return ret
}

fun Context.getList(): String {
    return Settings.Secure.getStringForUser(
        this.getContentResolver(),
        this.getString(R.string.key_icon_hide_list),
        UserHandle.USER_CURRENT,
    ) ?: this.getString(R.string.config_default_icon_hide_list)
}

fun Context.setList(hideList: ArraySet<String>) {
    this.setList(TextUtils.join(",", hideList))
}

fun Context.setList(hideList: String) {
    this.setStringSecure(this.getString(R.string.key_icon_hide_list), hideList)
}

fun Context.setStringSecure(key: String, value: String) {
    Settings.Secure.putStringForUser(this.contentResolver, key, value, UserHandle.USER_CURRENT)
}

fun Context.setBooleanSecure(key: String, value: Boolean) {
    Settings.Secure.putIntForUser(
        this.contentResolver,
        key,
        if (value) 1 else 0,
        UserHandle.USER_CURRENT,
    )
}

fun Context.getBooleanSecure(key: String, defaultValue: Boolean): Boolean {
    return Settings.Secure.getIntForUser(
        this.contentResolver,
        key,
        if (defaultValue) 1 else 0,
        UserHandle.USER_CURRENT,
    ) != 0
}

fun Context.setBooleanSystem(key: String, value: Boolean) {
    Settings.System.putIntForUser(
        this.contentResolver,
        key,
        if (value) 1 else 0,
        UserHandle.USER_CURRENT,
    )
}

fun Context.getBooleanSystem(key: String, defaultValue: Boolean): Boolean {
    return Settings.System.getIntForUser(
        this.contentResolver,
        key,
        if (defaultValue) 1 else 0,
        UserHandle.USER_CURRENT,
    ) != 0
}

fun Context.reset(initialize: Boolean) {
    if (initialize) {
        this.getDePrefs().edit().clear().apply()
    }

    this.setList(this.getString(R.string.config_default_icon_hide_list))

    this.setBooleanSecure(
        this.getString(R.string.key_clock_seconds),
        this.resources.getBoolean(R.bool.config_default_clock_seconds),
    )

    this.setBooleanSystem(
        SHOW_BATTERY_PERCENT,
        this.resources.getBoolean(R.bool.config_default_show_battery_percent),
    )

    this.setBooleanSecure(
        this.getString(R.string.key_low_priority),
        this.resources.getBoolean(R.bool.config_default_low_priority),
    )

    this.setBooleanSystem(
        this.getString(R.string.key_data_disabled_icon),
        this.resources.getBoolean(R.bool.config_default_data_disabled_icon),
    )

    this.setBooleanSystem(
        this.getString(R.string.key_show_fourg_icon),
        this.resources.getBoolean(R.bool.config_default_show_fourg_icon),
    )

    this.setBooleanSystem(
        this.getString(R.string.key_carrier_on_lockscreen),
        this.resources.getBoolean(R.bool.config_default_carrier_on_lockscreen),
    )
}
