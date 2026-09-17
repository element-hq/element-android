/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vector_settings")

class VectorDataStore @Inject constructor(
        private val context: Context
) {

    private val pushCounter = intPreferencesKey("push_counter")

    val pushCounterFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[pushCounter] ?: 0
    }

    suspend fun incrementPushCounter() {
        context.dataStore.edit { settings ->
            val currentCounterValue = settings[pushCounter] ?: 0
            settings[pushCounter] = currentCounterValue + 1
        }
    }

    private val elementXMigrationBannerTimestamp = longPreferencesKey("element_x_migration_banner_timestamp")

    val elementXMigrationBannerTimestampFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[elementXMigrationBannerTimestamp] ?: 0L
    }

    suspend fun setElementXMigrationBannerTimestamp(timestamp: Long) {
        context.dataStore.edit { settings ->
            settings[elementXMigrationBannerTimestamp] = timestamp
        }
    }
}
