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

package im.vector.app.fdroid.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import im.vector.app.core.services.GuardServiceStarter
import im.vector.app.features.settings.VectorPreferences
import timber.log.Timber
import javax.inject.Inject

class FDroidGuardServiceStarter @Inject constructor(
        private val preferences: VectorPreferences,
        private val appContext: Context
) : GuardServiceStarter {

    override fun start() {
        if (preferences.isBackgroundSyncEnabled()) {
            try {
                Timber.i("## Sync: starting GuardService")
                val intent = Intent(appContext, GuardService::class.java)
                ContextCompat.startForegroundService(appContext, intent)
            } catch (ex: Throwable) {
                Timber.e("## Sync: ERROR starting GuardService")
            }
        }
    }

    override fun stop() {
        val intent = Intent(appContext, GuardService::class.java)
        appContext.stopService(intent)
    }
}
