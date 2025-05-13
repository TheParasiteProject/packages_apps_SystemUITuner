/*
 * SPDX-FileCopyrightText: 2014-2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2018 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner

import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import android.util.AttributeSet
import com.android.systemui.tuner.preference.SelfRemovingSwitchPreference

open class TunerSwitch : SelfRemovingSwitchPreference {
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int,
    ) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context, null)

    override open fun isPersisted(): Boolean =
        Settings.Secure.getString(getContext().getContentResolver(), getKey()) != null

    protected override open fun putBoolean(key: String, value: Boolean) {
        Settings.Secure.putIntForUser(
            getContext().getContentResolver(),
            key,
            if (value) 1 else 0,
            UserHandle.USER_CURRENT,
        )
    }

    protected override open fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return Settings.Secure.getIntForUser(
            getContext().getContentResolver(),
            key,
            if (defaultValue) 1 else 0,
            UserHandle.USER_CURRENT,
        ) != 0
    }
}
