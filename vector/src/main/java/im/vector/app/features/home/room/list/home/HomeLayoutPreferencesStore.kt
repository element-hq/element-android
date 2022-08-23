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

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.extensions.orFalse
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "layout_preferences")

class HomeLayoutPreferencesStore @Inject constructor(
        private val context: Context
) {

    private val areRecentsEnbabled = booleanPreferencesKey("SETTINGS_PREFERENCES_HOME_RECENTS")
    private val areFiltersEnabled = booleanPreferencesKey("SETTINGS_PREFERENCES_HOME_FILTERS")
    private val isAZOrderingEnabled = booleanPreferencesKey("SETTINGS_PREFERENCES_USE_AZ_ORDER")

    val areRecentsEnabledFlow: Flow<Boolean> = context.dataStore.data
            .map { preferences -> preferences[areRecentsEnbabled].orFalse() }
            .distinctUntilChanged()

    val areFiltersEnabledFlow: Flow<Boolean> = context.dataStore.data
            .map { preferences -> preferences[areFiltersEnabled].orFalse() }
            .distinctUntilChanged()

    val isAZOrderingEnabledFlow: Flow<Boolean> = context.dataStore.data
            .map { preferences -> preferences[isAZOrderingEnabled].orFalse() }
            .distinctUntilChanged()

    suspend fun setRecentsEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[areRecentsEnbabled] = isEnabled
        }
    }

    suspend fun setFiltersEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[areFiltersEnabled] = isEnabled
        }
    }

    suspend fun setAZOrderingEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[isAZOrderingEnabled] = isEnabled
        }
    }
}
