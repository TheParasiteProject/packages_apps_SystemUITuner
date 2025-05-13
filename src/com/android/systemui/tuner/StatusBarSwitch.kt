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
import com.android.internal.logging.MetricsLogger
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.systemui.tuner.preference.SelfRemovingSwitchPreference

class StatusBarSwitch : SelfRemovingSwitchPreference {

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int,
    ) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context, null)

    private val mHideList: ArraySet<String>

    init {
        val blacklist =
            Settings.Secure.getStringForUser(
                getContext().getContentResolver(),
                ICON_HIDE_LIST,
                UserHandle.USER_CURRENT,
            ) ?: ""
        mHideList = context.getIconHideList(blacklist)
    }

    override open fun isPersisted(): Boolean = true

    protected override open fun putBoolean(key: String, value: Boolean) {
        if (!value) {
            // If not enabled add to hideList.
            if (!mHideList.contains(key)) {
                MetricsLogger.action(getContext(), MetricsEvent.TUNER_STATUS_BAR_DISABLE, key)
                mHideList.add(key)
                setList(mHideList)
            }
        } else {
            if (mHideList.remove(key)) {
                MetricsLogger.action(getContext(), MetricsEvent.TUNER_STATUS_BAR_ENABLE, key)
                setList(mHideList)
            }
        }
    }

    protected override open fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return !mHideList.contains(key)
    }

    private fun setList(hideList: ArraySet<String>) {
        Settings.Secure.putStringForUser(
            getContext().getContentResolver(),
            ICON_HIDE_LIST,
            TextUtils.join(",", hideList),
            UserHandle.USER_CURRENT,
        )
    }
}
