/*
 * SPDX-FileCopyrightText: The Android Open Source Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner

import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import android.provider.Settings.System.SHOW_BATTERY_PERCENT
import android.text.TextUtils
import android.util.AttributeSet
import androidx.collection.ArraySet
import com.android.internal.logging.MetricsLogger
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.systemui.tuner.preference.SelfRemovingListPreference

class BatteryPreference : SelfRemovingListPreference {

    private val mBattery: String
    private var mBatteryEnabled = false
    private var mHasPercentage = false
    private val mHideList: ArraySet<String>

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
        setEntryValues(arrayOf<CharSequence>(PERCENT, DEFAULT, DISABLED))
        mBattery = context.getString(com.android.internal.R.string.status_bar_battery)
        mHasPercentage =
            Settings.System.getIntForUser(
                getContext().getContentResolver(),
                SHOW_BATTERY_PERCENT,
                0,
                UserHandle.USER_CURRENT,
            ) != 0
    }

    override open fun isPersisted(): Boolean = true

    override open fun putString(key: String, value: String?) {
        when (value) {
            PERCENT -> {
                if (!mHasPercentage) {
                    Settings.System.putIntForUser(
                        getContext().getContentResolver(),
                        SHOW_BATTERY_PERCENT,
                        1,
                        UserHandle.USER_CURRENT,
                    )
                    mHasPercentage = true
                }
                if (mHideList.contains(mBattery)) {
                    mHideList.remove(mBattery)
                    setList(mHideList)
                    mBatteryEnabled = true
                    MetricsLogger.action(getContext(), MetricsEvent.TUNER_BATTERY_PERCENTAGE, true)
                }
            }
            DEFAULT -> {
                if (mHasPercentage) {
                    Settings.System.putIntForUser(
                        getContext().getContentResolver(),
                        SHOW_BATTERY_PERCENT,
                        0,
                        UserHandle.USER_CURRENT,
                    )
                    mHasPercentage = false
                }
                if (mHideList.contains(mBattery)) {
                    mHideList.remove(mBattery)
                    setList(mHideList)
                    mBatteryEnabled = true
                    MetricsLogger.action(getContext(), MetricsEvent.TUNER_BATTERY_PERCENTAGE, false)
                }
            }
            DISABLED -> {
                if (mHasPercentage) {
                    Settings.System.putIntForUser(
                        getContext().getContentResolver(),
                        SHOW_BATTERY_PERCENT,
                        0,
                        UserHandle.USER_CURRENT,
                    )
                    mHasPercentage = false
                }
                if (!mHideList.contains(mBattery)) {
                    mHideList.add(mBattery)
                    setList(mHideList)
                    mBatteryEnabled = false
                    MetricsLogger.action(getContext(), MetricsEvent.TUNER_BATTERY_PERCENTAGE, false)
                }
            }
        }
    }

    override open fun getString(key: String, defaultValue: String?): String {
        return when {
            mBatteryEnabled && mHasPercentage -> PERCENT
            mBatteryEnabled -> DEFAULT
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
        private const val PERCENT = "percent"
        private const val DEFAULT = "default"
        private const val DISABLED = "disabled"
    }
}
