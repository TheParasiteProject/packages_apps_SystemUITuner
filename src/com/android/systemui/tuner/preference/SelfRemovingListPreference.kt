/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2018 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceViewHolder

/**
 * A Preference which can automatically remove itself from the hierarchy based on constraints set in
 * XML.
 */
abstract class SelfRemovingListPreference : ListPreference {
    private val mConstraints: ConstraintsHelper

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

    protected abstract fun putString(key: String, value: String?)

    protected abstract fun getString(key: String, defaultValue: String?): String

    protected override open fun onSetInitialValue(
        restorePersistedValue: Boolean,
        defaultValue: Any?,
    ) {
        val value: String
        if (!restorePersistedValue || !isPersisted()) {
            if (defaultValue == null) {
                return
            }
            value = defaultValue as String
            if (shouldPersist()) {
                persistString(value)
            }
        } else {
            // Note: the default is not used because to have got here
            // isPersisted() must be true.
            value = getString(getKey(), null /* not used */)
        }
        setValue(value)
    }

    private inner class DataStore : PreferenceDataStore() {
        override fun putString(key: String, value: String?) {
            this@SelfRemovingListPreference.putString(key, value)
        }

        override fun getString(key: String, defaultValue: String?): String {
            return this@SelfRemovingListPreference.getString(key, defaultValue)
        }
    }
}
