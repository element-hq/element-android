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

package im.vector.app.features.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import im.vector.app.core.extensions.dataStoreProvider
import im.vector.app.features.onboarding.FtueUseCase
import kotlinx.coroutines.flow.first
import org.matrix.android.sdk.api.util.md5

/**
 * User session scoped storage for:
 * - messaging use case (Enum/String)
 */
class VectorSessionStore constructor(
        context: Context,
        myUserId: String
) {

    private val useCaseKey = stringPreferencesKey("use_case")
    private val dataStore by lazy { context.dataStoreProvider("vector_session_store_${myUserId.md5()}") }

    suspend fun readUseCase() = dataStore.data.first().let { preferences ->
        preferences[useCaseKey]?.let { FtueUseCase.from(it) }
    }

    suspend fun setUseCase(useCase: FtueUseCase) {
        dataStore.edit { settings ->
            settings[useCaseKey] = useCase.persistableValue
        }
    }

    suspend fun resetUseCase() {
        dataStore.edit { settings ->
            settings.remove(useCaseKey)
        }
    }

    suspend fun clear() {
        dataStore.edit { settings -> settings.clear() }
    }
}
