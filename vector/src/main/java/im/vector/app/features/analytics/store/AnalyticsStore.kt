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

package im.vector.app.features.analytics.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.extensions.orFalse
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vector_analytics")

/**
 * Local storage for:
 * - user consent (Boolean)
 * - did ask user consent (Boolean)
 * - analytics Id (String)
 */
class AnalyticsStore @Inject constructor(
        private val context: Context
) {
    private val userConsent = booleanPreferencesKey("user_consent")
    private val didAskUserConsent = booleanPreferencesKey("did_ask_user_consent")
    private val analyticsId = stringPreferencesKey("analytics_id")

    val userConsentFlow: Flow<Boolean> = context.dataStore.data
            .map { preferences -> preferences[userConsent].orFalse() }
            .distinctUntilChanged()

    val didAskUserConsentFlow: Flow<Boolean> = context.dataStore.data
            .map { preferences -> preferences[didAskUserConsent].orFalse() }
            .distinctUntilChanged()

    val analyticsIdFlow: Flow<String> = context.dataStore.data
            .map { preferences -> preferences[analyticsId].orEmpty() }
            .distinctUntilChanged()

    suspend fun setUserConsent(newUserConsent: Boolean) {
        context.dataStore.edit { settings ->
            settings[userConsent] = newUserConsent
        }
    }

    suspend fun setDidAskUserConsent(newValue: Boolean = true) {
        context.dataStore.edit { settings ->
            settings[didAskUserConsent] = newValue
        }
    }

    suspend fun setAnalyticsId(newAnalyticsId: String) {
        context.dataStore.edit { settings ->
            settings[analyticsId] = newAnalyticsId
        }
    }
}
