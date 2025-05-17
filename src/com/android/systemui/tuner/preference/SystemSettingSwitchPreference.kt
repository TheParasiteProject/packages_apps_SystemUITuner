/*
 * SPDX-FileCopyrightText: 2013 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2018 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner.preference

import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import android.util.AttributeSet
import com.android.systemui.tuner.TunerService
import com.android.systemui.tuner.TunerService.Tunable

open class SystemSettingSwitchPreference : SelfRemovingSwitchPreference, Tunable {

    private var mEnabled = false

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int,
    ) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context, null)

    override fun onTuningChanged(key: String, newValue: String?) {
        refreshPreference()
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
        TunerService.get().addTunable(this, "system:${getKey()}")
    }

    override open fun onDetached() {
        super.onDetached()
        TunerService.get().removeTunable(this)
    }

    protected override open fun isPersisted(): Boolean =
        Settings.System.getStringForUser(
            getContext().getContentResolver(),
            getKey(),
            UserHandle.USER_CURRENT,
        ) != null

    protected override open fun putBoolean(key: String, value: Boolean) {
        Settings.System.putIntForUser(
            getContext().getContentResolver(),
            key,
            if (value) 1 else 0,
            UserHandle.USER_CURRENT,
        )
    }

    protected override open fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return Settings.System.getIntForUser(
            getContext().getContentResolver(),
            key,
            if (defaultValue) 1 else 0,
            UserHandle.USER_CURRENT,
        ) != 0
    }
}
