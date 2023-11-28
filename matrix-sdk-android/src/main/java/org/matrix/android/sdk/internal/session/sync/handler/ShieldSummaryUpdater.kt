/*
 * Copyright (c) 2023 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.sync.handler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.internal.crypto.ComputeShieldForGroupUseCase
import org.matrix.android.sdk.internal.crypto.CryptoSessionInfoProvider
import org.matrix.android.sdk.internal.crypto.OlmMachine
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class ShieldSummaryUpdater @Inject constructor(
        private val olmMachine: dagger.Lazy<OlmMachine>,
        private val coroutineScope: CoroutineScope,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoSessionInfoProvider: CryptoSessionInfoProvider,
        private val computeShieldForGroup: ComputeShieldForGroupUseCase,
) {

    fun refreshShieldsForRoomsWithMembers(userIds: List<String>) {
        coroutineScope.launch(coroutineDispatchers.computation) {
            cryptoSessionInfoProvider.getRoomsWhereUsersAreParticipating(userIds).forEach { roomId ->
                if (cryptoSessionInfoProvider.isRoomEncrypted(roomId)) {
                    val userGroup = cryptoSessionInfoProvider.getUserListForShieldComputation(roomId)
                    val shield = computeShieldForGroup(olmMachine.get(), userGroup)
                    cryptoSessionInfoProvider.updateShieldForRoom(roomId, shield)
                } else {
                    cryptoSessionInfoProvider.updateShieldForRoom(roomId, null)
                }
            }
        }
    }

    fun refreshShieldsForRoomIds(roomIds: Set<String>) {
        coroutineScope.launch(coroutineDispatchers.computation) {
            roomIds.forEach { roomId ->
                val userGroup = cryptoSessionInfoProvider.getUserListForShieldComputation(roomId)
                val shield = computeShieldForGroup(olmMachine.get(), userGroup)
                cryptoSessionInfoProvider.updateShieldForRoom(roomId, shield)
            }
        }
    }
}
