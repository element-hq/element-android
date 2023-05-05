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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.flow.flow
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class RefreshDevicesOnCryptoDevicesChangeUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {
    private val samplingPeriodMs = 5.seconds.inWholeMilliseconds

    suspend fun execute() {
        activeSessionHolder.getSafeActiveSession()
                ?.let { session ->
                    session.flow().liveUserCryptoDevices(session.myUserId)
                            .map { it.size }
                            .distinctUntilChanged()
                            .sample(samplingPeriodMs)
                            .onEach {
                                // If we have a new crypto device change, we might want to trigger refresh of device info
                                tryOrNull { session.cryptoService().fetchDevicesList() }
                            }
                            .collect()
                }
    }
}
