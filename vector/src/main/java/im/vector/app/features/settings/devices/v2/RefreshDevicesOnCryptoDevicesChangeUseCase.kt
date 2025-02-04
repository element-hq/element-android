/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
