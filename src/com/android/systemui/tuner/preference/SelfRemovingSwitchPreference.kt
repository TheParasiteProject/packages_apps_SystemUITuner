/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2018-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat

/**
 * A SwitchPreferenceCompat which can automatically remove itself from the hierarchy based on
 * constraints set in XML.
 */
abstract class SelfRemovingSwitchPreference : SwitchPreferenceCompat {
    private val mConstraints: ConstraintsHelper
    protected var defValue = false

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int,
    ) : super(context, attrs, defStyle) {
        mConstraints = ConstraintsHelper(context, attrs, this)
        setPreferenceDataStore(DataStore())
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mConstraints = ConstraintsHelper(context, attrs, this)
        setPreferenceDataStore(DataStore())
    }

    constructor(context: Context) : super(context) {
        mConstraints = ConstraintsHelper(context, null, this)
        setPreferenceDataStore(DataStore())
    }

    override open fun onAttached() {
        super.onAttached()
        mConstraints.onAttached()
    }

    override open fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        mConstraints.onBindViewHolder(holder)
    }

    open fun setAvailable(available: Boolean) {
        mConstraints.setAvailable(available)
    }

    open fun isAvailable(): Boolean = mConstraints.isAvailable()

    protected abstract fun isPersisted(): Boolean

    protected abstract fun putBoolean(key: String, value: Boolean)

    protected abstract fun getBoolean(key: String, defaultValue: Boolean): Boolean

    protected override open fun onSetInitialValue(
        restorePersistedValue: Boolean,
        defaultValue: Any?,
    ) {
        val checked: Boolean
        if (!restorePersistedValue || !isPersisted()) {
            defValue = if (defaultValue == null) false else defaultValue as Boolean
            checked = getBoolean(getKey(), defValue)
            if (shouldPersist()) {
                persistBoolean(checked)
            }
        } else {
            // Note: the default is not used because to have got here
            // isPersisted() must be true.
            checked = getBoolean(getKey(), false /* not used */)
        }
        setChecked(checked)
    }

    private inner class DataStore : PreferenceDataStore() {
        override fun putBoolean(key: String, value: Boolean) {
            this@SelfRemovingSwitchPreference.putBoolean(key, value)
        }

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
            return this@SelfRemovingSwitchPreference.getBoolean(key, defaultValue)
        }
    }
}
