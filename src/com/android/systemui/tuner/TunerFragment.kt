/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.tuner

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.preference.PreferenceFragmentCompat

class TunerFragment :
    PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var prefs: SharedPreferences
    private lateinit var mContext: Context
    private lateinit var mMenuProvider: TunerMenuProvider

    private val MENU_RESET = 1

    private inner class TunerMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menu.add(Menu.NONE, MENU_RESET, Menu.NONE, R.string.menu_restore)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            if (menuItem.itemId == MENU_RESET) {
                AlertDialog.Builder(mContext)
                    .setTitle(mContext.getString(R.string.reset_tuner_settings_confirm_title))
                    .setMessage(mContext.getString(R.string.reset_tuner_settings_desc))
                    .setPositiveButton(R.string.reset, { dialog, which -> context?.reset(true) })
                    .setNegativeButton(R.string.cancel, null)
                    .show()
                return true
            }
            return false
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tuner_prefs, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager.setStorageDeviceProtected()
        preferenceManager.sharedPreferencesName = PREFS_NAME

        mContext = requireContext()
        prefs = mContext.getDePrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)

        mMenuProvider = TunerMenuProvider()
        activity?.addMenuProvider(mMenuProvider)
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.removeMenuProvider(mMenuProvider)

        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {}
}
