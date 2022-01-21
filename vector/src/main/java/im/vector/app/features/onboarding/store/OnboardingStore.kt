/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.onboarding.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import im.vector.app.core.extensions.removeKeysWithPrefix
import im.vector.app.features.onboarding.FtueUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vector_onboarding")

/**
 * Local storage for:
 * - messaging use case (Enum/String)
 */
class OnboardingStore @Inject constructor(
        private val context: Context
) {
    suspend fun readUseCase(userId: String) = context.dataStore.data.first().let { preferences ->
        preferences[userId.toUseCaseKey()]?.let { FtueUseCase.from(it) }
    }

    suspend fun setUseCase(userId: String, useCase: FtueUseCase) {
        context.dataStore.edit { settings ->
            settings[userId.toUseCaseKey()] = useCase.persistableValue
        }
    }

    suspend fun resetUseCase(userId: String) {
        context.dataStore.edit { settings ->
            settings.remove(userId.toUseCaseKey())
        }
    }

    suspend fun clear(userId: String) {
        context.dataStore.removeKeysWithPrefix(userId)
    }

    private fun String.toUseCaseKey() = stringPreferencesKey("$this-use_case")
}
