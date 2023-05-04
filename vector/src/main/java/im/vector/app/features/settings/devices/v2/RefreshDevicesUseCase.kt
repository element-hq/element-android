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

package im.vector.app.features.settings.devices.v2

import im.vector.app.core.di.ActiveSessionHolder
import org.matrix.android.sdk.api.extensions.tryOrNull
import javax.inject.Inject

class RefreshDevicesUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {
    suspend fun execute() {
        activeSessionHolder.getSafeActiveSession()?.let { session ->
            tryOrNull { session.cryptoService().fetchDevicesList() }
            session.cryptoService().downloadKeysIfNeeded(listOf(session.myUserId), true)
        }
    }
}
