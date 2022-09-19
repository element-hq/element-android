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

package im.vector.app.features.home.room.list.home.release

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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "release_notes")

class ReleaseNotesPreferencesStore @Inject constructor(
        private val context: Context
) {

    private val isAppLayoutOnboardingShown = booleanPreferencesKey("SETTINGS_APP_LAYOUT_ONBOARDING_DISPLAYED")

    val appLayoutOnboardingShown: Flow<Boolean> = context.dataStore.data
            .map { preferences -> preferences[isAppLayoutOnboardingShown].orFalse() }
            .distinctUntilChanged()

    suspend fun setAppLayoutOnboardingShown(isShown: Boolean) {
        context.dataStore.edit { settings ->
            settings[isAppLayoutOnboardingShown] = isShown
        }
    }
}
