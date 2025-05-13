/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.tuner

import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.System.SHOW_BATTERY_PERCENT
import androidx.preference.PreferenceFragmentCompat

class TunerFragment :
    PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var prefs: SharedPreferences
    private lateinit var mContext: Context
    private lateinit var settingsObserver: SettingsObserver

    private class SettingsObserver(handler: Handler, val onChangeCallback: () -> Unit) :
        ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            if (selfChange) return
            onChangeCallback.invoke()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tuner_prefs, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager.setStorageDeviceProtected()
        preferenceManager.sharedPreferencesName = PREFS_NAME

        mContext = requireContext()
        prefs = mContext.getDePrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)

        settingsObserver =
            SettingsObserver(Handler(Looper.getMainLooper())) { refreshAllPreferences() }
        mContext.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(ICON_HIDE_LIST),
            false,
            settingsObserver,
        )
        mContext.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(CLOCK_SECONDS),
            false,
            settingsObserver,
        )
        mContext.contentResolver.registerContentObserver(
            Settings.System.getUriFor(SHOW_BATTERY_PERCENT),
            false,
            settingsObserver,
        )
        mContext.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(LOW_PRIORITY),
            false,
            settingsObserver,
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        mContext.contentResolver.unregisterContentObserver(settingsObserver)
    }

    private fun refreshAllPreferences() {
        preferenceScreen.removeAll()
        setPreferencesFromResource(R.xml.tuner_prefs, null)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {}
}
