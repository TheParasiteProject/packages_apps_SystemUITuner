/*
 * SPDX-FileCopyrightText: The Android Open Source Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner

import android.content.Context
import android.provider.Settings.System.SHOW_BATTERY_PERCENT
import android.util.AttributeSet
import androidx.collection.ArraySet
import com.android.internal.logging.MetricsLogger
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.systemui.tuner.TunerService.Tunable
import com.android.systemui.tuner.preference.SelfRemovingListPreference

class BatteryPreference : SelfRemovingListPreference, Tunable {

    private val mBattery: String
    private var mBatteryEnabled = false
    private var mHasPercentage = false
    private lateinit var mHideList: ArraySet<String>
    private val ICON_HIDE_LIST: String

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int,
    ) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context, null)

    init {
        ICON_HIDE_LIST = context.getString(R.string.key_icon_hide_list)

        mBattery = context.getString(com.android.internal.R.string.status_bar_battery)
        setEntryValues(arrayOf<CharSequence>(PERCENT, DEFAULT, DISABLED))
        refreshPreference()
    }

    override fun onTuningChanged(key: String, newValue: String?) {
        refreshPreference()
    }

    private fun updateBlackList() {
        mHideList = context.getIconHideList(context.getList())
        mBatteryEnabled = !mHideList.contains(mBattery)
    }

    private fun updateHasPercentage() {
        mHasPercentage =
            context.getBooleanSystem(
                SHOW_BATTERY_PERCENT,
                context.resources.getBoolean(R.bool.config_default_show_battery_percent),
            )
    }

    private fun refreshPreference() {
        updateBlackList()
        updateHasPercentage()
        setValue(getStringInternal())
    }

    override open fun onAttached() {
        super.onAttached()
        TunerService.get().addTunable(this, ICON_HIDE_LIST, "system:${SHOW_BATTERY_PERCENT}")
    }

    override open fun onDetached() {
        super.onDetached()
        TunerService.get().removeTunable(this)
    }

    override open fun isPersisted(): Boolean = true

    override open fun putString(key: String, value: String?) {
        when (value) {
            PERCENT -> {
                if (!mHasPercentage) {
                    context.setBooleanSystem(SHOW_BATTERY_PERCENT, true)
                    mHasPercentage = true
                }
                if (mHideList.contains(mBattery)) {
                    mHideList.remove(mBattery)
                    context.setList(mHideList)
                    mBatteryEnabled = true
                    MetricsLogger.action(context, MetricsEvent.TUNER_BATTERY_PERCENTAGE, true)
                }
            }
            DEFAULT -> {
                if (mHasPercentage) {
                    context.setBooleanSystem(SHOW_BATTERY_PERCENT, false)
                    mHasPercentage = false
                }
                if (mHideList.contains(mBattery)) {
                    mHideList.remove(mBattery)
                    context.setList(mHideList)
                    mBatteryEnabled = true
                    MetricsLogger.action(context, MetricsEvent.TUNER_BATTERY_PERCENTAGE, false)
                }
            }
            DISABLED -> {
                if (mHasPercentage) {
                    context.setBooleanSystem(SHOW_BATTERY_PERCENT, false)
                    mHasPercentage = false
                }
                if (!mHideList.contains(mBattery)) {
                    mHideList.add(mBattery)
                    context.setList(mHideList)
                    mBatteryEnabled = false
                    MetricsLogger.action(context, MetricsEvent.TUNER_BATTERY_PERCENTAGE, false)
                }
            }
        }
    }

    private fun getStringInternal(): String {
        return when {
            mBatteryEnabled && mHasPercentage -> PERCENT
            mBatteryEnabled -> DEFAULT
            else -> DISABLED
        }
    }

    override open fun getString(key: String, defaultValue: String?): String {
        return getStringInternal()
    }

    companion object {
        private const val PERCENT = "percent"
        private const val DEFAULT = "default"
        private const val DISABLED = "disabled"
    }
}
