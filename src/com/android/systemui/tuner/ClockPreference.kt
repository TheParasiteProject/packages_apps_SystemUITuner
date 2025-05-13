/*
 * SPDX-FileCopyrightText: The Android Open Source Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner

import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import android.text.TextUtils
import android.util.AttributeSet
import androidx.collection.ArraySet
import com.android.systemui.tuner.preference.SelfRemovingListPreference

class ClockPreference : SelfRemovingListPreference {

    private var mClockEnabled = false
    private var mHasSeconds = false
    private val mHideList: ArraySet<String>
    private var mHasSetValue = false
    private val mClock: String

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int,
    ) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context, null)

    init {
        val blacklist =
            Settings.Secure.getStringForUser(
                getContext().getContentResolver(),
                ICON_HIDE_LIST,
                UserHandle.USER_CURRENT,
            ) ?: ""
        mHideList = context.getIconHideList(blacklist)
        setEntryValues(arrayOf<CharSequence>(SECONDS, DEFAULT, DISABLED))
        mClock = context.getString(com.android.internal.R.string.status_bar_clock)
        mHasSeconds =
            Settings.Secure.getIntForUser(
                getContext().getContentResolver(),
                CLOCK_SECONDS,
                0 /* Showing seconds is disabled by default */,
                UserHandle.USER_CURRENT,
            ) != 0
        mClockEnabled = !mHideList.contains(mClock)
    }

    override open fun isPersisted(): Boolean = true

    override open fun putString(key: String, value: String?) {
        when (value) {
            SECONDS -> {
                if (!mHasSeconds) {
                    Settings.Secure.putIntForUser(
                        getContext().getContentResolver(),
                        CLOCK_SECONDS,
                        1,
                        UserHandle.USER_CURRENT,
                    )
                    mHasSeconds = true
                }
                if (mHideList.contains(mClock)) {
                    mHideList.remove(mClock)
                    setList(mHideList)
                    mClockEnabled = true
                }
            }
            DEFAULT -> {
                if (mHasSeconds) {
                    Settings.Secure.putIntForUser(
                        getContext().getContentResolver(),
                        CLOCK_SECONDS,
                        0,
                        UserHandle.USER_CURRENT,
                    )
                    mHasSeconds = false
                }
                if (mHideList.contains(mClock)) {
                    mHideList.remove(mClock)
                    setList(mHideList)
                    mClockEnabled = true
                }
            }
            DISABLED -> {
                if (mHasSeconds) {
                    Settings.Secure.putIntForUser(
                        getContext().getContentResolver(),
                        CLOCK_SECONDS,
                        0,
                        UserHandle.USER_CURRENT,
                    )
                    mHasSeconds = false
                }
                if (!mHideList.contains(mClock)) {
                    mHideList.add(mClock)
                    setList(mHideList)
                    mClockEnabled = false
                }
            }
        }
    }

    override open fun getString(key: String, defaultValue: String?): String {
        return when {
            mClockEnabled && mHasSeconds -> SECONDS
            mClockEnabled -> DEFAULT
            else -> DISABLED
        }
    }

    private fun setList(hideList: ArraySet<String>) {
        Settings.Secure.putStringForUser(
            getContext().getContentResolver(),
            ICON_HIDE_LIST,
            TextUtils.join(",", hideList),
            UserHandle.USER_CURRENT,
        )
    }

    companion object {
        private const val SECONDS = "seconds"
        private const val DEFAULT = "default"
        private const val DISABLED = "disabled"
    }
}
