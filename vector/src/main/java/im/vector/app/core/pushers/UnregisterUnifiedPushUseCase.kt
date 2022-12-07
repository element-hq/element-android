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

package im.vector.app.core.pushers

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import im.vector.app.features.settings.BackgroundSyncMode
import im.vector.app.features.settings.VectorPreferences
import org.unifiedpush.android.connector.UnifiedPush
import timber.log.Timber
import javax.inject.Inject

class UnregisterUnifiedPushUseCase @Inject constructor(
        @ApplicationContext private val context: Context,
        private val vectorPreferences: VectorPreferences,
        private val unifiedPushStore: UnifiedPushStore,
        private val unifiedPushHelper: UnifiedPushHelper,
) {

    suspend fun execute(pushersManager: PushersManager?) {
        val mode = BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_REALTIME
        vectorPreferences.setFdroidSyncBackgroundMode(mode)
        try {
            unifiedPushHelper.getEndpointOrToken()?.let {
                Timber.d("Removing $it")
                pushersManager?.unregisterPusher(it)
            }
        } catch (e: Exception) {
            Timber.d(e, "Probably unregistering a non existing pusher")
        }
        unifiedPushStore.storeUpEndpoint(null)
        unifiedPushStore.storePushGateway(null)
        UnifiedPush.unregisterApp(context)
    }
}
