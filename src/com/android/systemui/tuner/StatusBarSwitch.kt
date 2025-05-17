/*
 * SPDX-FileCopyrightText: The Android Open Source Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner

import android.content.Context
import android.util.AttributeSet
import androidx.collection.ArraySet
import com.android.internal.logging.MetricsLogger
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.systemui.tuner.TunerService.Tunable
import com.android.systemui.tuner.preference.SelfRemovingSwitchPreference

class StatusBarSwitch : SelfRemovingSwitchPreference, Tunable {

    private var mEnabled = false
    private val ICON_HIDE_LIST: String

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int,
    ) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context, null)

    private lateinit var mHideList: ArraySet<String>

    init {
        ICON_HIDE_LIST = context.getString(R.string.key_icon_hide_list)

        updateBlackList()
    }

    override fun onTuningChanged(key: String, newValue: String?) {
        refreshPreference()
    }

    private fun updateBlackList() {
        mHideList = context.getIconHideList(context.getList())
        mEnabled = !mHideList.contains(getKey())
    }

    private fun refreshPreference() {
        updateBlackList()
        setChecked(mEnabled)
    }

    override open fun onAttached() {
        super.onAttached()
        TunerService.get().addTunable(this, ICON_HIDE_LIST)
    }

    override open fun onDetached() {
        super.onDetached()
        TunerService.get().removeTunable(this)
    }

    override open fun isPersisted(): Boolean = true

    protected override open fun putBoolean(key: String, value: Boolean) {
        if (!value) {
            // If not enabled add to hideList.
            if (!mHideList.contains(key)) {
                MetricsLogger.action(getContext(), MetricsEvent.TUNER_STATUS_BAR_DISABLE, key)
                mHideList.add(key)
                context.setList(mHideList)
            }
        } else {
            if (mHideList.remove(key)) {
                MetricsLogger.action(getContext(), MetricsEvent.TUNER_STATUS_BAR_ENABLE, key)
                context.setList(mHideList)
            }
        }
    }

    protected override open fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return !mHideList.contains(key)
    }
}
