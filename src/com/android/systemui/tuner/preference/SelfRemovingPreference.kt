/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

/**
 * A Preference which can automatically remove itself from the hierarchy based on constraints set in
 * XML.
 */
open class SelfRemovingPreference : Preference {
    private val mConstraints: ConstraintsHelper

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyle, defStyleRes) {
        mConstraints = ConstraintsHelper(context, attrs, this)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int,
    ) : this(context, attrs, defStyle, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
    ) : this(
        context,
        attrs,
        ConstraintsHelper.getAttr(
            context,
            androidx.preference.R.attr.preferenceStyle,
            android.R.attr.preferenceStyle,
        ),
    )

    constructor(context: Context) : this(context, null)

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
}
