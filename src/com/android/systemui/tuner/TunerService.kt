/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.tuner

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.provider.Settings.Secure
import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import java.util.concurrent.ConcurrentHashMap
import lineageos.providers.LineageSettings

class TunerService private constructor(private val context: Context) {
    private val mObserver: Observer = Observer()
    // Map of Uris we listen on to their settings keys.
    private val mListeningUris: ArrayMap<Uri, String> = ArrayMap<Uri, String>()
    // Map of settings keys to the listener.
    private val mTunableLookup: ConcurrentHashMap<String, MutableSet<Tunable>> =
        ConcurrentHashMap<String, MutableSet<Tunable>>()

    private val mCurrentUser: Int = UserHandle.getUserId(UserHandle.USER_CURRENT)
    private val mContentResolver: ContentResolver = context.contentResolver

    interface Tunable {
        fun onTuningChanged(key: String, newValue: String?)
    }

    private fun isLineageSetting(key: String): Boolean {
        return isLineageGlobal(key) || isLineageSystem(key) || isLineageSecure(key)
    }

    private fun isLineageGlobal(key: String): Boolean {
        return key.startsWith("lineageglobal:")
    }

    private fun isLineageSystem(key: String): Boolean {
        return key.startsWith("lineagesystem:")
    }

    private fun isLineageSecure(key: String): Boolean {
        return key.startsWith("lineagesecure:")
    }

    private fun isSystem(key: String): Boolean {
        return key.startsWith("system:")
    }

    private fun isGlobal(key: String): Boolean {
        return key.startsWith("global:")
    }

    private fun chomp(key: String): String {
        return key.replaceFirst(
            "^(lineageglobal|lineagesecure|lineagesystem|system|global):".toRegex(),
            "",
        )
    }

    fun getValue(setting: String): String? {
        return when {
            isLineageGlobal(setting) -> {
                LineageSettings.Global.getString(mContentResolver, chomp(setting))
            }
            isLineageSecure(setting) -> {
                LineageSettings.Secure.getStringForUser(
                    mContentResolver,
                    chomp(setting),
                    mCurrentUser,
                )
            }
            isLineageSystem(setting) -> {
                LineageSettings.System.getStringForUser(
                    mContentResolver,
                    chomp(setting),
                    mCurrentUser,
                )
            }
            isSystem(setting) -> {
                Settings.System.getStringForUser(mContentResolver, chomp(setting), mCurrentUser)
            }
            isGlobal(setting) -> {
                Settings.Global.getStringForUser(mContentResolver, chomp(setting), mCurrentUser)
            }
            else -> {
                Settings.Secure.getStringForUser(mContentResolver, setting, mCurrentUser)
            }
        }
    }

    fun setValue(setting: String, value: String) {
        when {
            isLineageGlobal(setting) -> {
                LineageSettings.Global.putString(mContentResolver, chomp(setting), value)
            }
            isLineageSecure(setting) -> {
                LineageSettings.Secure.putStringForUser(
                    mContentResolver,
                    chomp(setting),
                    value,
                    mCurrentUser,
                )
            }
            isLineageSystem(setting) -> {
                LineageSettings.System.putStringForUser(
                    mContentResolver,
                    chomp(setting),
                    value,
                    mCurrentUser,
                )
            }
            isSystem(setting) -> {
                Settings.System.putStringForUser(
                    mContentResolver,
                    chomp(setting),
                    value,
                    mCurrentUser,
                )
            }
            isGlobal(setting) -> {
                Settings.Global.putStringForUser(
                    mContentResolver,
                    chomp(setting),
                    value,
                    mCurrentUser,
                )
            }
            else -> {
                Settings.Secure.putStringForUser(mContentResolver, setting, value, mCurrentUser)
            }
        }
    }

    fun getValue(setting: String, def: Int): Int {
        return when {
            isLineageGlobal(setting) -> {
                LineageSettings.Global.getInt(mContentResolver, chomp(setting), def)
            }
            isLineageSecure(setting) -> {
                LineageSettings.Secure.getIntForUser(
                    mContentResolver,
                    chomp(setting),
                    def,
                    mCurrentUser,
                )
            }
            isLineageSystem(setting) -> {
                LineageSettings.System.getIntForUser(
                    mContentResolver,
                    chomp(setting),
                    def,
                    mCurrentUser,
                )
            }
            isSystem(setting) -> {
                Settings.System.getIntForUser(mContentResolver, chomp(setting), def, mCurrentUser)
            }
            isGlobal(setting) -> {
                Settings.Global.getInt(mContentResolver, chomp(setting), def)
            }
            else -> {
                Settings.Secure.getIntForUser(mContentResolver, setting, def, mCurrentUser)
            }
        }
    }

    fun getValue(setting: String, def: String): String {
        val ret: String? =
            when {
                isLineageGlobal(setting) -> {
                    LineageSettings.Global.getString(mContentResolver, chomp(setting))
                }
                isLineageSecure(setting) -> {
                    LineageSettings.Secure.getStringForUser(
                        mContentResolver,
                        chomp(setting),
                        mCurrentUser,
                    )
                }
                isLineageSystem(setting) -> {
                    LineageSettings.System.getStringForUser(
                        mContentResolver,
                        chomp(setting),
                        mCurrentUser,
                    )
                }
                isSystem(setting) -> {
                    Settings.System.getStringForUser(mContentResolver, chomp(setting), mCurrentUser)
                }
                isGlobal(setting) -> {
                    Settings.Global.getStringForUser(mContentResolver, chomp(setting), mCurrentUser)
                }
                else -> {
                    Settings.Secure.getStringForUser(mContentResolver, setting, mCurrentUser)
                }
            }
        if (ret == null) return def
        return ret
    }

    fun setValue(setting: String, value: Int) {
        when {
            isLineageGlobal(setting) -> {
                LineageSettings.Global.putInt(mContentResolver, chomp(setting), value)
            }
            isLineageSecure(setting) -> {
                LineageSettings.Secure.putIntForUser(
                    mContentResolver,
                    chomp(setting),
                    value,
                    mCurrentUser,
                )
            }
            isLineageSystem(setting) -> {
                LineageSettings.System.putIntForUser(
                    mContentResolver,
                    chomp(setting),
                    value,
                    mCurrentUser,
                )
            }
            isSystem(setting) -> {
                Settings.System.putIntForUser(mContentResolver, chomp(setting), value, mCurrentUser)
            }
            isGlobal(setting) -> {
                Settings.Global.putInt(mContentResolver, chomp(setting), value)
            }
            else -> {
                Settings.Secure.putIntForUser(mContentResolver, setting, value, mCurrentUser)
            }
        }
    }

    fun destroy() {
        mContentResolver.unregisterContentObserver(mObserver)
    }

    fun addTunable(tunable: Tunable, vararg keys: String) {
        for (key in keys) {
            addTunable(tunable, key)
        }
    }

    private fun addTunable(tunable: Tunable, key: String) {
        if (!mTunableLookup.containsKey(key)) {
            mTunableLookup.put(key, ArraySet<Tunable>())
        }
        mTunableLookup.get(key)?.add(tunable)
        val uri: Uri =
            when {
                isLineageGlobal(key) -> LineageSettings.Global.getUriFor(chomp(key))
                isLineageSecure(key) -> LineageSettings.Secure.getUriFor(chomp(key))
                isLineageSystem(key) -> LineageSettings.System.getUriFor(chomp(key))
                isSystem(key) -> Settings.System.getUriFor(chomp(key))
                isGlobal(key) -> Settings.Global.getUriFor(chomp(key))
                else -> Settings.Secure.getUriFor(key)
            }
        synchronized(this) {
            if (!mListeningUris.containsKey(uri)) {
                mListeningUris.put(uri, key)
                mContentResolver.registerContentObserver(
                    uri,
                    false,
                    mObserver,
                    if (isLineageGlobal(key)) UserHandle.USER_ALL else mCurrentUser,
                )
            }
        }
        tunable.onTuningChanged(key, getValue(key))
    }

    fun removeTunable(tunable: Tunable) {
        for (list in mTunableLookup.values) {
            list.remove(tunable)
        }
    }

    private fun reloadSetting(uri: Uri) {
        val key: String? = mListeningUris.get(uri) ?: return
        val tunables: Set<Tunable>? = mTunableLookup.get(key)
        if (tunables == null) {
            return
        }
        val value: String? = getValue(key!!)
        for (tunable in tunables) {
            tunable.onTuningChanged(key!!, value)
        }
    }

    private inner class Observer : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uris: Collection<Uri>, flags: Int, userId: Int) {
            for (u in uris) {
                val key: String = mListeningUris.get(u) ?: continue
                if (userId == mCurrentUser || isLineageGlobal(key)) {
                    reloadSetting(u)
                }
            }
        }
    }

    companion object {
        private var instance: TunerService? = null
        private var _context: Context? = null

        fun initialize(context: Context) {
            if (_context == null) {
                _context = context
            }
        }

        fun get(): TunerService {
            val ctx = _context
            if (instance == null) {
                instance = TunerService(ctx!!)
            }
            return instance!!
        }

        fun destroyInstance() {
            instance?.destroy()
            instance = null
            _context = null
        }

        fun parseIntegerSwitch(value: String, defaultValue: Boolean): Boolean {
            try {
                return if (value != null) value.toInt() != 0 else defaultValue
            } catch (e: NumberFormatException) {
                return defaultValue
            }
        }

        fun parseInteger(value: String, defaultValue: Int): Int {
            try {
                return if (value != null) value.toInt() else defaultValue
            } catch (e: NumberFormatException) {
                return defaultValue
            }
        }
    }
}
