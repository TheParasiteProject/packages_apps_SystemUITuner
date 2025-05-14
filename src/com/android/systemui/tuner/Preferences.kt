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
import android.text.TextUtils
import androidx.collection.ArraySet
import androidx.collection.arraySetOf

const val PREFS_NAME = "systemui_tuner_preferences"
const val PACKAGE_SYSTEMUI = "com.android.systemui"

const val ICON_HIDE_LIST = "icon_blacklist"
const val CLOCK_SECONDS = "clock_seconds"
const val LOW_PRIORITY = "low_priority"

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

fun Context.setList(hideList: String) {
    Settings.Secure.putStringForUser(
        this.getContentResolver(),
        this.getString(R.string.key_icon_hide_list),
        hideList,
        UserHandle.USER_CURRENT,
    )
}

fun Context.setBooleanSecure(key: String, value: Boolean) {
    Settings.Secure.putIntForUser(
        this.getContentResolver(),
        key,
        if (value) 1 else 0,
        UserHandle.USER_CURRENT,
    )
}

fun Context.setBooleanSystem(key: String, value: Boolean) {
    Settings.System.putIntForUser(
        this.getContentResolver(),
        key,
        if (value) 1 else 0,
        UserHandle.USER_CURRENT,
    )
}
