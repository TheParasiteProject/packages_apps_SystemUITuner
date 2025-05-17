/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner

import android.content.Context
import android.content.SharedPreferences
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
        prefs = context.getDePrefs()
    }

    private inner class DataStore : PreferenceDataStore() {
        fun save() {
            val blacklist = context.getList()

            val hasSeconds =
                context.getBooleanSecure(
                    CLOCK_SECONDS,
                    context.resources.getBoolean(R.bool.config_default_clock_seconds),
                )
            val hasPercentage =
                context.getBooleanSystem(
                    SHOW_BATTERY_PERCENT,
                    context.resources.getBoolean(R.bool.config_default_show_battery_percent),
                )
            val lowPriority =
                context.getBooleanSecure(
                    LOW_PRIORITY,
                    context.resources.getBoolean(R.bool.config_default_low_priority),
                )

            val dataDisabledIcon =
                context.getBooleanSystem(
                    context.getString(R.string.key_data_disabled_icon),
                    context.resources.getBoolean(R.bool.config_default_data_disabled_icon),
                )
            val showFourgIcon =
                context.getBooleanSystem(
                    context.getString(R.string.key_show_fourg_icon),
                    context.resources.getBoolean(R.bool.config_default_show_fourg_icon),
                )
            val carrierOnLockscreen =
                context.getBooleanSystem(
                    context.getString(R.string.key_carrier_on_lockscreen),
                    context.resources.getBoolean(R.bool.config_default_carrier_on_lockscreen),
                )

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
        }

        fun restore() {
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
                    context.resources.getBoolean(R.bool.config_default_clock_seconds),
                ),
            )

            context.setBooleanSystem(
                SHOW_BATTERY_PERCENT,
                prefs.getBoolean(
                    SHOW_BATTERY_PERCENT,
                    context.resources.getBoolean(R.bool.config_default_show_battery_percent),
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
        }

        override fun putBoolean(key: String, value: Boolean) {
            prefs.setEnabled(context, value)

            if (value) {
                restore()
                return
            }

            save()
            context.reset(false)
        }

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
            return prefs.getEnabled(context)
        }
    }
}
