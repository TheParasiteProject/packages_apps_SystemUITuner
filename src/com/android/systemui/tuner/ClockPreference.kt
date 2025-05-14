/*
 * SPDX-FileCopyrightText: The Android Open Source Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.text.TextUtils
import android.util.AttributeSet
import androidx.collection.ArraySet
import com.android.systemui.tuner.preference.SelfRemovingListPreference

class ClockPreference : SelfRemovingListPreference {

    private var mClockEnabled = false
    private var mHasSeconds = false
    private lateinit var mHideList: ArraySet<String>
    private var mHasSetValue = false
    private val mClock: String
    private val settingsObserver: SettingsObserver

    private class SettingsObserver(handler: Handler, val onChangeCallback: () -> Unit) :
        ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            onChangeCallback.invoke()
        }
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int,
    ) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context, null)

    init {
        mClock = context.getString(com.android.internal.R.string.status_bar_clock)
        setEntryValues(arrayOf<CharSequence>(SECONDS, DEFAULT, DISABLED))
        refreshPreference()

        settingsObserver = SettingsObserver(Handler(Looper.getMainLooper())) { refreshPreference() }
    }

    private fun updateBlackList() {
        val blacklist =
            Settings.Secure.getStringForUser(
                getContext().getContentResolver(),
                ICON_HIDE_LIST,
                UserHandle.USER_CURRENT,
            ) ?: context.getString(R.string.config_default_icon_hide_list)
        mHideList = context.getIconHideList(blacklist)
        mClockEnabled = !mHideList.contains(mClock)
    }

    private fun updateHasSeconds() {
        mHasSeconds =
            Settings.Secure.getIntForUser(
                getContext().getContentResolver(),
                CLOCK_SECONDS,
                context.resources.getInteger(R.integer.config_default_clock_seconds),
                UserHandle.USER_CURRENT,
            ) != 0
    }

    private fun refreshPreference() {
        updateBlackList()
        updateHasSeconds()
        setValue(getStringInternal())
    }

    override open fun onAttached() {
        super.onAttached()
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(ICON_HIDE_LIST),
            false,
            settingsObserver,
        )
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(CLOCK_SECONDS),
            false,
            settingsObserver,
        )
    }

    override open fun onDetached() {
        super.onDetached()
        context.contentResolver.unregisterContentObserver(settingsObserver)
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
