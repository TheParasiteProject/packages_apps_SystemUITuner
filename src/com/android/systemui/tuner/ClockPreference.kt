/*
 * SPDX-FileCopyrightText: The Android Open Source Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner

import android.content.Context
import android.util.AttributeSet
import androidx.collection.ArraySet
import com.android.systemui.tuner.TunerService.Tunable
import com.android.systemui.tuner.preference.SelfRemovingListPreference

class ClockPreference : SelfRemovingListPreference, Tunable {

    private var mClockEnabled = false
    private var mHasSeconds = false
    private lateinit var mHideList: ArraySet<String>
    private var mHasSetValue = false
    private val mClock: String
    private val ICON_HIDE_LIST: String
    private val CLOCK_SECONDS: String

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int,
    ) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context, null)

    init {
        ICON_HIDE_LIST = context.getString(R.string.key_icon_hide_list)
        CLOCK_SECONDS = context.getString(R.string.key_clock_seconds)

        mClock = context.getString(com.android.internal.R.string.status_bar_clock)
        setEntryValues(arrayOf<CharSequence>(SECONDS, DEFAULT, DISABLED))
        refreshPreference()
    }

    override fun onTuningChanged(key: String, newValue: String?) {
        refreshPreference()
    }

    private fun updateBlackList() {
        mHideList = context.getIconHideList(context.getList())
        mClockEnabled = !mHideList.contains(mClock)
    }

    private fun updateHasSeconds() {
        mHasSeconds =
            context.getBooleanSecure(
                CLOCK_SECONDS,
                context.resources.getBoolean(R.bool.config_default_clock_seconds),
            )
    }

    private fun refreshPreference() {
        updateBlackList()
        updateHasSeconds()
        setValue(getStringInternal())
    }

    override open fun onAttached() {
        super.onAttached()
        TunerService.get().addTunable(this, ICON_HIDE_LIST, CLOCK_SECONDS)
    }

    override open fun onDetached() {
        super.onDetached()
        TunerService.get().removeTunable(this)
    }

    override open fun isPersisted(): Boolean = true

    override open fun putString(key: String, value: String?) {
        when (value) {
            SECONDS -> {
                if (!mHasSeconds) {
                    context.setBooleanSecure(CLOCK_SECONDS, true)
                    mHasSeconds = true
                }
                if (mHideList.contains(mClock)) {
                    mHideList.remove(mClock)
                    context.setList(mHideList)
                    mClockEnabled = true
                }
            }
            DEFAULT -> {
                if (mHasSeconds) {
                    context.setBooleanSecure(CLOCK_SECONDS, false)
                    mHasSeconds = false
                }
                if (mHideList.contains(mClock)) {
                    mHideList.remove(mClock)
                    context.setList(mHideList)
                    mClockEnabled = true
                }
            }
            DISABLED -> {
                if (mHasSeconds) {
                    context.setBooleanSecure(CLOCK_SECONDS, false)
                    mHasSeconds = false
                }
                if (!mHideList.contains(mClock)) {
                    mHideList.add(mClock)
                    context.setList(mHideList)
                    mClockEnabled = false
                }
            }
        }
    }

    private fun getStringInternal(): String {
        return when {
            mClockEnabled && mHasSeconds -> SECONDS
            mClockEnabled -> DEFAULT
            else -> DISABLED
        }
    }

    override open fun getString(key: String, defaultValue: String?): String {
        return getStringInternal()
    }

    companion object {
        private const val SECONDS = "seconds"
        private const val DEFAULT = "default"
        private const val DISABLED = "disabled"
    }
}
