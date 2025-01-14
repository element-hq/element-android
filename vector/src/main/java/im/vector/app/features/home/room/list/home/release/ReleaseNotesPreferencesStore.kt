/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
