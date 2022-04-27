/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.settings

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.settings.LightweightSettingsStorage
import org.matrix.android.sdk.internal.session.sync.SyncPresence
import javax.inject.Inject

/**
 * The purpose of this class is to provide an alternative and lightweight way to store settings/data
 * on the sdk without using the database. This should be used just for sdk/user preferences and
 * not for large data sets
 */
internal class DefaultLightweightSettingsStorage @Inject constructor(
        context: Context,
        private val matrixConfiguration: MatrixConfiguration
) : LightweightSettingsStorage {

    private val sdkDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    override fun setThreadMessagesEnabled(enabled: Boolean) {
        sdkDefaultPrefs.edit {
            putBoolean(MATRIX_SDK_SETTINGS_THREAD_MESSAGES_ENABLED, enabled)
        }
    }

    override fun areThreadMessagesEnabled(): Boolean {
        return sdkDefaultPrefs.getBoolean(MATRIX_SDK_SETTINGS_THREAD_MESSAGES_ENABLED, matrixConfiguration.threadMessagesEnabledDefault)
    }

    /**
     * Set the presence status sent on syncs when the application is in foreground.
     *
     * @param presence the presence status that should be sent on sync
     */
    internal fun setSyncPresenceStatus(presence: SyncPresence) {
        sdkDefaultPrefs.edit {
            putString(MATRIX_SDK_SETTINGS_FOREGROUND_PRESENCE_STATUS, presence.value)
        }
    }

    /**
     * Get the presence status that should be sent on syncs when the application is in foreground.
     *
     * @return the presence status that should be sent on sync
     */
    internal fun getSyncPresenceStatus(): SyncPresence {
        val presenceString = sdkDefaultPrefs.getString(MATRIX_SDK_SETTINGS_FOREGROUND_PRESENCE_STATUS, SyncPresence.Online.value)
        return SyncPresence.from(presenceString) ?: SyncPresence.Online
    }

    companion object {
        const val MATRIX_SDK_SETTINGS_THREAD_MESSAGES_ENABLED = "MATRIX_SDK_SETTINGS_THREAD_MESSAGES_ENABLED"
        private const val MATRIX_SDK_SETTINGS_FOREGROUND_PRESENCE_STATUS = "MATRIX_SDK_SETTINGS_FOREGROUND_PRESENCE_STATUS"
    }
}
