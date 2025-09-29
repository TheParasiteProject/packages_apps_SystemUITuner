/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017,2019-2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner.preference

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.TypedArray
import android.os.IBinder
import android.os.ServiceManager
import android.os.SystemProperties
import android.os.UserHandle
import android.telephony.TelephonyManager
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import com.android.systemui.tuner.R
import com.android.systemui.tuner.R.styleable.lineage_SelfRemovingPreference_minSummaryLines
import com.android.systemui.tuner.R.styleable.lineage_SelfRemovingPreference_replacesKey

/**
 * Helpers for checking if a device supports various features.
 *
 * @hide
 */
class ConstraintsHelper(
    private val context: Context,
    private val attrs: AttributeSet?,
    private val pref: Preference,
) {
    private var verifyIntent: Boolean = true

    private var summaryMinLines: Int = -1

    private var replacesKey: List<String>? = null

    private var available: Boolean = true

    init {
        val a: TypedArray =
            context.resources.obtainAttributes(attrs, R.styleable.lineage_SelfRemovingPreference)
        try {
            summaryMinLines = a.getInteger(lineage_SelfRemovingPreference_minSummaryLines, -1)
            val replacesKeys: String? = a.getString(lineage_SelfRemovingPreference_replacesKey)
            if (replacesKeys != null) {
                replacesKey = replacesKeys.split("\\|")
            }
        } finally {
            a.recycle()
        }
        available = checkConstraints()

        Log.d(TAG, "construct key=${pref.key} available=${available}")
    }

    fun setVerifyIntent(value: Boolean) {
        this.verifyIntent = value
    }

    fun isAvailable(): Boolean = available

    fun setAvailable(value: Boolean) {
        this.available = value
        if (!value) {
            Graveyard.get(context).addTombstone(pref.key)
        }
    }

    private fun getParent(preference: Preference): PreferenceGroup? {
        return getParent(pref.preferenceManager.preferenceScreen, preference)
    }

    private fun getParent(root: PreferenceGroup, preference: Preference): PreferenceGroup? {
        for (i in 0 until root.preferenceCount) {
            val p: Preference? = root.getPreference(i)
            if (p == preference) return root
            if (p is PreferenceGroup) {
                val parent = getParent(p, preference)
                if (parent != null) return parent
            }
        }
        return null
    }

    private fun isNegated(key: String?): Boolean {
        return key?.startsWith("!") == true
    }

    private fun checkIntent() {
        if (!verifyIntent) return

        val i: Intent? = pref.intent
        if (i != null) {
            if (!resolveIntent(context, i)) {
                Graveyard.get(context).addTombstone(pref.key)
                available = false
            }
        }
    }

    private fun checkConstraints(): Boolean {
        if (attrs == null) {
            return true
        }

        val a: TypedArray =
            context
                .getResources()
                .obtainAttributes(attrs, R.styleable.lineage_SelfRemovingPreference)

        try {
            // Check if the current user is an owner
            val rOwner: Boolean =
                a.getBoolean(R.styleable.lineage_SelfRemovingPreference_requiresOwner, false)
            if (rOwner && UserHandle.myUserId() != UserHandle.USER_SYSTEM) {
                return false
            }

            // Check if a specific package is installed
            var rPackage: String? =
                a.getString(R.styleable.lineage_SelfRemovingPreference_requiresPackage)
            if (rPackage != null) {
                val negated: Boolean = isNegated(rPackage)
                if (negated) {
                    rPackage = rPackage.substring(1)
                }
                val available: Boolean = isPackageInstalled(context, rPackage, false)
                if (available == negated) {
                    return false
                }
            }

            // Check if an intent can be resolved to handle the given action
            var rAction: String? =
                a.getString(R.styleable.lineage_SelfRemovingPreference_requiresAction)
            if (rAction != null) {
                val negated: Boolean = isNegated(rAction)
                if (negated) {
                    rAction = rAction.substring(1)
                }
                val available: Boolean = resolveIntent(context, rAction)
                if (available == negated) {
                    return false
                }
            }

            // Check if a system feature is available
            var rFeature: String? =
                a.getString(R.styleable.lineage_SelfRemovingPreference_requiresFeature)
            if (rFeature != null) {
                val negated: Boolean = isNegated(rFeature)
                if (negated) {
                    rFeature = rFeature.substring(1)
                }
                val available: Boolean = hasSystemFeature(context, rFeature)
                if (available == negated) {
                    return false
                }
            }

            // Check a boolean system property
            var rProperty: String? =
                a.getString(R.styleable.lineage_SelfRemovingPreference_requiresProperty)
            if (rProperty != null) {
                val negated: Boolean = isNegated(rProperty)
                if (negated) {
                    rProperty = rProperty.substring(1)
                }
                val value: String? = SystemProperties.get(rProperty)
                val available = value != null && value.equals("true", ignoreCase = true)
                if (available == negated) {
                    return false
                }
            }

            // Check a config resource. This can be a bool, string or integer.
            // The preference is removed if any of the following are true:
            // * A bool resource is false.
            // * A string resource is null.
            // * An integer resource is zero.
            // * An integer is non-zero and when bitwise logically ANDed with
            //   attribute requiresConfigMask, the result is zero.
            val tv: TypedValue? =
                a.peekValue(R.styleable.lineage_SelfRemovingPreference_requiresConfig)
            if (tv != null && tv.resourceId != 0) {
                when {
                    (tv.type == TypedValue.TYPE_STRING &&
                        context.resources.getString(tv.resourceId) == null) ||
                        (tv.type == TypedValue.TYPE_INT_BOOLEAN && tv.data == 0) -> return false
                    tv.type == TypedValue.TYPE_INT_DEC -> {
                        val mask =
                            a.getInt(
                                R.styleable.lineage_SelfRemovingPreference_requiresConfigMask,
                                -1,
                            )
                        if (tv.data == 0 || (mask >= 0 && (tv.data and mask) == 0)) {
                            return false
                        }
                    }
                    else -> {}
                }
            }

            // Check a system service
            var rService: String? =
                a.getString(R.styleable.lineage_SelfRemovingPreference_requiresService)
            if (rService != null) {
                val negated: Boolean = isNegated(rService)
                if (negated) {
                    rService = rService.substring(1)
                }
                val value: IBinder? = ServiceManager.getService(rService)
                val available = value != null
                if (available == negated) {
                    return false
                }
            }
        } finally {
            a.recycle()
        }

        return true
    }

    fun onAttached() {
        checkIntent()

        if (isAvailable() && replacesKey != null) {
            Graveyard.get(context).addTombstones(replacesKey)
        }

        Graveyard.get(context).summonReaper(pref.preferenceManager)
    }

    fun onBindViewHolder(holder: PreferenceViewHolder) {
        if (!isAvailable()) {
            return
        }

        if (summaryMinLines > 0) {
            val textView: TextView? =
                holder.itemView.findViewById(android.R.id.summary) as? TextView
            if (textView != null) {
                textView.minLines = summaryMinLines
            }
        }
    }

    /**
     * If we want to keep this at the preference level vs the fragment level, we need to collate all
     * the preferences that need to be removed when attached to the hierarchy, then purge them all
     * when loading is complete. The Graveyard keeps track of this, and will reap the dead when
     * onAttached is called.
     */
    private class Graveyard private constructor(private val context: Context) {

        private val deathRowLock = Any()

        private var deathRow: MutableSet<String> = mutableSetOf<String>()

        fun addTombstone(pref: String?) {
            if (pref == null) return
            synchronized(deathRowLock) { deathRow.add(pref) }
        }

        fun addTombstones(prefs: List<String>?) {
            if (prefs == null) return
            synchronized(deathRowLock) { deathRow.addAll(prefs) }
        }

        private fun getParent(p1: Preference, p2: Preference): PreferenceGroup? {
            return getParent(p1.preferenceManager.preferenceScreen, p2)
        }

        private fun getParent(root: PreferenceGroup, preference: Preference): PreferenceGroup? {
            for (i in 0 until root.preferenceCount) {
                val p: Preference? = root.getPreference(i)
                if (p == preference) return root
                if (p is PreferenceGroup) {
                    val parent = getParent(p, preference)
                    if (parent != null) return parent
                }
            }
            return null
        }

        private fun hidePreference(mgr: PreferenceManager, pref: Preference) {
            pref.setVisible(false)
            // Hide the group if nothing is visible
            val group: PreferenceGroup? = getParent(pref, pref)
            if (group == null) return
            var allHidden: Boolean = true
            for (i in 0 until group.preferenceCount) {
                if (group.getPreference(i).isVisible()) {
                    allHidden = false
                    break
                }
            }
            if (allHidden) {
                group.setVisible(false)
            }
        }

        fun summonReaper(mgr: PreferenceManager) {
            synchronized(deathRowLock) {
                val notReadyForReap: MutableSet<String> = mutableSetOf<String>()
                for (dead in deathRow) {
                    val deadPref: Preference? = mgr.findPreference(dead)
                    if (deadPref != null) {
                        hidePreference(mgr, deadPref)
                    } else {
                        notReadyForReap.add(dead)
                    }
                }
                deathRow = notReadyForReap
            }
        }

        companion object {
            @Volatile private var sInstance: Graveyard? = null

            fun get(context: Context) =
                sInstance
                    ?: synchronized(this) {
                        sInstance ?: Graveyard(context.applicationContext).also { sInstance = it }
                    }
        }
    }

    companion object {
        private const val TAG: String = "ConstraintsHelper"
        private val DEBUG: Boolean = Log.isLoggable(TAG, Log.VERBOSE)

        /** Returns whether the device supports a particular feature */
        @JvmStatic
        fun hasSystemFeature(context: Context, feature: String): Boolean {
            return context.packageManager.hasSystemFeature(feature)
        }

        /** Returns whether the device is voice-capable (meaning, it is also a phone). */
        @JvmStatic
        fun isVoiceCapable(context: Context): Boolean {
            val telephony: TelephonyManager? =
                context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            return telephony?.isVoiceCapable() == true
        }

        /**
         * Checks if a package is installed. Set the ignoreState argument to true if you don't care
         * if the package is enabled/disabled.
         */
        @JvmStatic
        fun isPackageInstalled(context: Context, pkg: String?, ignoreState: Boolean): Boolean {
            if (pkg != null) {
                try {
                    val pi: PackageInfo? = context.packageManager.getPackageInfo(pkg, 0)
                    if (pi?.applicationInfo?.enabled == false && !ignoreState) {
                        return false
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    return false
                }
            }

            return true
        }

        /** Checks if a package is available to handle the given action. */
        @JvmStatic
        fun resolveIntent(context: Context, intent: Intent): Boolean {
            if (DEBUG) Log.d(TAG, "resolveIntent ${intent.toString()}")
            // check whether the target handler exist in system
            val pm: PackageManager = context.packageManager
            val results: List<ResolveInfo> =
                pm.queryIntentActivitiesAsUser(
                    intent,
                    PackageManager.MATCH_SYSTEM_ONLY,
                    UserHandle.myUserId(),
                )
            for (resolveInfo in results) {
                // check is it installed in system.img, exclude the application
                // installed by user
                if (DEBUG) Log.d(TAG, "resolveInfo: ${resolveInfo.toString()}")
                if (
                    (resolveInfo.activityInfo.applicationInfo.flags and
                        ApplicationInfo.FLAG_SYSTEM) != 0
                ) {
                    return true
                }
            }
            return false
        }

        @JvmStatic
        fun resolveIntent(context: Context, action: String): Boolean {
            return resolveIntent(context, Intent(action))
        }

        @JvmStatic
        fun getAttr(context: Context, attr: Int, fallbackAttr: Int): Int {
            var value: TypedValue = TypedValue()
            context.theme.resolveAttribute(attr, value, true)
            if (value.resourceId != 0) {
                return attr
            }
            return fallbackAttr
        }
    }
}
