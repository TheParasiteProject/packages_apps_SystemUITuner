/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner

import android.content.Context
import android.content.SharedPreferences
import android.os.UserHandle
import android.provider.Settings
import android.provider.Settings.System.SHOW_BATTERY_PERCENT
import android.util.AttributeSet
import androidx.preference.PreferenceDataStore
import com.android.settingslib.widget.MainSwitchPreference

class TunerMainSwitchPreference : MainSwitchPreference {

    private val prefs: SharedPreferences
    private val ICON_HIDE_LIST: String
    private val CLOCK_SECONDS: String
    private val LOW_PRIORITY: String

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int,
    ) : super(context, attrs, defStyle) {
        setPreferenceDataStore(DataStore())
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setPreferenceDataStore(DataStore())
    }

    constructor(context: Context) : super(context, null) {
        setPreferenceDataStore(DataStore())
    }

    init {
        ICON_HIDE_LIST = context.getString(R.string.key_icon_hide_list)
        CLOCK_SECONDS = context.getString(R.string.key_clock_seconds)
        LOW_PRIORITY = context.getString(R.string.key_low_priority)
        prefs = getContext().getDePrefs()
    }

    private inner class DataStore : PreferenceDataStore() {
        override fun putBoolean(key: String, value: Boolean) {

            prefs.setEnabled(context, value)

            if (value) {
                context.setList(
                    prefs.getString(
                        ICON_HIDE_LIST,
                        context.getString(R.string.config_default_icon_hide_list),
                    ) ?: context.getString(R.string.config_default_icon_hide_list)
                )

                context.setBooleanSecure(
                    CLOCK_SECONDS,
                    prefs.getBoolean(
                        CLOCK_SECONDS,
                        context.resources.getInteger(R.integer.config_default_clock_seconds) != 0,
                    ),
                )

                context.setBooleanSystem(
                    SHOW_BATTERY_PERCENT,
                    prefs.getBoolean(
                        SHOW_BATTERY_PERCENT,
                        context.resources.getInteger(
                            R.integer.config_default_show_battery_percent
                        ) != 0,
                    ),
                )

                context.setBooleanSecure(
                    LOW_PRIORITY,
                    prefs.getBoolean(
                        LOW_PRIORITY,
                        context.resources.getBoolean(R.bool.config_default_low_priority),
                    ),
                )

                context.setBooleanSystem(
                    context.getString(R.string.key_data_disabled_icon),
                    prefs.getBoolean(
                        context.getString(R.string.key_data_disabled_icon),
                        context.resources.getBoolean(R.bool.config_default_data_disabled_icon),
                    ),
                )

                context.setBooleanSystem(
                    context.getString(R.string.key_show_fourg_icon),
                    prefs.getBoolean(
                        context.getString(R.string.key_show_fourg_icon),
                        context.resources.getBoolean(R.bool.config_default_show_fourg_icon),
                    ),
                )

                context.setBooleanSystem(
                    context.getString(R.string.key_carrier_on_lockscreen),
                    prefs.getBoolean(
                        context.getString(R.string.key_carrier_on_lockscreen),
                        context.resources.getBoolean(R.bool.config_default_carrier_on_lockscreen),
                    ),
                )
                return
            }

            val blacklist =
                Settings.Secure.getStringForUser(
                    context.getContentResolver(),
                    ICON_HIDE_LIST,
                    UserHandle.USER_CURRENT,
                ) ?: context.getString(R.string.config_default_icon_hide_list)

            val hasSeconds =
                Settings.Secure.getIntForUser(
                    context.getContentResolver(),
                    CLOCK_SECONDS,
                    context.resources.getInteger(R.integer.config_default_clock_seconds),
                    UserHandle.USER_CURRENT,
                ) != 0

            val hasPercentage =
                Settings.System.getIntForUser(
                    context.getContentResolver(),
                    SHOW_BATTERY_PERCENT,
                    context.resources.getInteger(R.integer.config_default_show_battery_percent),
                    UserHandle.USER_CURRENT,
                ) != 0

            val lowPriority =
                Settings.Secure.getIntForUser(
                    context.getContentResolver(),
                    LOW_PRIORITY,
                    if (context.resources.getBoolean(R.bool.config_default_low_priority)) {
                        1
                    } else {
                        0
                    },
                    UserHandle.USER_CURRENT,
                ) != 0

            val dataDisabledIcon =
                Settings.System.getIntForUser(
                    context.getContentResolver(),
                    context.getString(R.string.key_data_disabled_icon),
                    if (context.resources.getBoolean(R.bool.config_default_data_disabled_icon)) {
                        1
                    } else {
                        0
                    },
                    UserHandle.USER_CURRENT,
                ) != 0

            val showFourgIcon =
                Settings.System.getIntForUser(
                    context.getContentResolver(),
                    context.getString(R.string.key_show_fourg_icon),
                    if (context.resources.getBoolean(R.bool.config_default_show_fourg_icon)) {
                        1
                    } else {
                        0
                    },
                    UserHandle.USER_CURRENT,
                ) != 0

            val carrierOnLockscreen =
                Settings.System.getIntForUser(
                    context.getContentResolver(),
                    context.getString(R.string.key_carrier_on_lockscreen),
                    if (context.resources.getBoolean(R.bool.config_default_carrier_on_lockscreen)) {
                        1
                    } else {
                        0
                    },
                    UserHandle.USER_CURRENT,
                ) != 0

            prefs.edit().putString(ICON_HIDE_LIST, blacklist).apply()
            prefs.edit().putBoolean(CLOCK_SECONDS, hasSeconds).apply()
            prefs.edit().putBoolean(SHOW_BATTERY_PERCENT, hasPercentage).apply()
            prefs.edit().putBoolean(LOW_PRIORITY, lowPriority).apply()
            prefs
                .edit()
                .putBoolean(context.getString(R.string.key_data_disabled_icon), dataDisabledIcon)
                .apply()
            prefs
                .edit()
                .putBoolean(context.getString(R.string.key_show_fourg_icon), showFourgIcon)
                .apply()
            prefs
                .edit()
                .putBoolean(
                    context.getString(R.string.key_carrier_on_lockscreen),
                    carrierOnLockscreen,
                )
                .apply()

            context.setList(context.getString(R.string.config_default_icon_hide_list))

            context.setBooleanSecure(
                CLOCK_SECONDS,
                context.resources.getInteger(R.integer.config_default_clock_seconds) != 0,
            )

            context.setBooleanSystem(
                SHOW_BATTERY_PERCENT,
                context.resources.getInteger(R.integer.config_default_show_battery_percent) != 0,
            )

            context.setBooleanSecure(
                LOW_PRIORITY,
                context.resources.getBoolean(R.bool.config_default_low_priority),
            )

            context.setBooleanSystem(
                context.getString(R.string.key_data_disabled_icon),
                context.resources.getBoolean(R.bool.config_default_data_disabled_icon),
            )

            context.setBooleanSystem(
                context.getString(R.string.key_show_fourg_icon),
                context.resources.getBoolean(R.bool.config_default_show_fourg_icon),
            )

            context.setBooleanSystem(
                context.getString(R.string.key_carrier_on_lockscreen),
                context.resources.getBoolean(R.bool.config_default_carrier_on_lockscreen),
            )
        }

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
            return prefs.getEnabled(context)
        }
    }
}
