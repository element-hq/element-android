/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.list.home

import android.content.SharedPreferences
import androidx.core.content.edit
import im.vector.app.core.di.DefaultPreferences
import javax.inject.Inject

class HomeLayoutPreferences @Inject constructor(
        @DefaultPreferences private val preferences: SharedPreferences
) {

    companion object {
        const val SETTINGS_PREFERENCES_HOME_RECENTS = "SETTINGS_PREFERENCES_HOME_RECENTS"
        const val SETTINGS_PREFERENCES_HOME_FILTERS = "SETTINGS_PREFERENCES_HOME_FILTERS"
        const val SETTINGS_PREFERENCES_USE_AZ_ORDER = "SETTINGS_PREFERENCES_USE_AZ_ORDER"
    }

    // We need to keep references, because it's kept as a Weak reference and so will be gathered by GC
    private var filtersListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var recentsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var orderListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun areRecentsEnabled(): Boolean {
        return preferences.getBoolean(SETTINGS_PREFERENCES_HOME_RECENTS, false)
    }

    fun setRecentsEnabled(isEnabled: Boolean) {
        preferences.edit {
            putBoolean(SETTINGS_PREFERENCES_HOME_RECENTS, isEnabled)
        }
    }

    fun areFiltersEnabled(): Boolean {
        return preferences.getBoolean(SETTINGS_PREFERENCES_HOME_FILTERS, false)
    }

    fun setFiltersEnabled(isEnabled: Boolean) {
        preferences.edit {
            putBoolean(SETTINGS_PREFERENCES_HOME_FILTERS, isEnabled)
        }
    }

    fun isAZOrderingEnabled(): Boolean {
        return preferences.getBoolean(SETTINGS_PREFERENCES_USE_AZ_ORDER, false)
    }

    fun setAZOrderingEnabled(isEnabled: Boolean) {
        preferences.edit {
            putBoolean(SETTINGS_PREFERENCES_USE_AZ_ORDER, isEnabled)
        }
    }

    fun registerFiltersListener(callBack: (Boolean) -> Unit) {
        filtersListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                SETTINGS_PREFERENCES_HOME_FILTERS -> {
                    callBack.invoke(areFiltersEnabled())
                }
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(filtersListener)
    }

    fun registerRecentsListener(callBack: (Boolean) -> Unit) {
        recentsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                SETTINGS_PREFERENCES_HOME_RECENTS -> {
                    callBack.invoke(areRecentsEnabled())
                }
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(recentsListener)
    }

    fun registerOrderingListener(callBack: (Boolean) -> Unit) {
        orderListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                SETTINGS_PREFERENCES_USE_AZ_ORDER -> {
                    callBack.invoke(isAZOrderingEnabled())
                }
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(orderListener)
    }

    fun unregisterListeners() {
        preferences.unregisterOnSharedPreferenceChangeListener(filtersListener)
        preferences.unregisterOnSharedPreferenceChangeListener(recentsListener)
        preferences.unregisterOnSharedPreferenceChangeListener(orderListener)
    }
}
