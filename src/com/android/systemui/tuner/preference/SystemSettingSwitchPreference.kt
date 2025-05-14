/*
 * SPDX-FileCopyrightText: 2013 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2018 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner.preference

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.util.AttributeSet

class SystemSettingSwitchPreference : SelfRemovingSwitchPreference {

    private var mEnabled = false

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int,
    ) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context, null)

    private val settingsObserver: SettingsObserver

    private class SettingsObserver(handler: Handler, val onChangeCallback: () -> Unit) :
        ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            onChangeCallback.invoke()
        }
    }

    init {
        settingsObserver = SettingsObserver(Handler(Looper.getMainLooper())) { refreshPreference() }
    }

    private fun updateValue() {
        mEnabled = getBoolean(getKey(), defValue)
    }

    private fun refreshPreference() {
        updateValue()
        setChecked(mEnabled)
    }

    override open fun onAttached() {
        super.onAttached()
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(getKey()),
            false,
            settingsObserver,
        )
    }

    override open fun onDetached() {
        super.onDetached()
        context.contentResolver.unregisterContentObserver(settingsObserver)
    }

    protected override fun isPersisted(): Boolean =
        Settings.System.getStringForUser(
            getContext().getContentResolver(),
            getKey(),
            UserHandle.USER_CURRENT,
        ) != null

    protected override fun putBoolean(key: String, value: Boolean) {
        Settings.System.putIntForUser(
            getContext().getContentResolver(),
            key,
            if (value) 1 else 0,
            UserHandle.USER_CURRENT,
        )
    }

    protected override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return Settings.System.getIntForUser(
            getContext().getContentResolver(),
            key,
            if (defaultValue) 1 else 0,
            UserHandle.USER_CURRENT,
        ) != 0
    }
}
