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

package org.matrix.android.sdk.internal.crypto

import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.crypto.network.OutgoingRequestsProcessor
import org.matrix.android.sdk.internal.crypto.network.RequestSender
import org.matrix.rustcomponents.sdk.crypto.Request
import org.matrix.rustcomponents.sdk.crypto.RequestType
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider

internal class EnsureUsersKeysUseCase @Inject constructor(
        private val olmMachine: Provider<OlmMachine>,
        private val outgoingRequestsProcessor: OutgoingRequestsProcessor,
        private val requestSender: RequestSender,
        private val coroutineDispatchers: MatrixCoroutineDispatchers) {

    suspend operator fun invoke(userIds: List<String>, forceDownload: Boolean) {
        val olmMachine = olmMachine.get()
        if (forceDownload) {
            tryOrNull("Failed to download keys for $userIds") {
                forceKeyDownload(olmMachine, userIds)
            }
        } else {
            userIds.filter { userId ->
                !olmMachine.isUserTracked(userId)
            }.also { untrackedUserIds ->
                olmMachine.updateTrackedUsers(untrackedUserIds)
            }
            outgoingRequestsProcessor.processOutgoingRequests(olmMachine) {
                it is Request.KeysQuery && it.users.intersect(userIds.toSet()).isNotEmpty()
            }
        }
    }

    @Throws
    private suspend fun forceKeyDownload(olmMachine: OlmMachine, userIds: List<String>) {
        withContext(coroutineDispatchers.io) {
            val requestId = UUID.randomUUID().toString()
            val response = requestSender.queryKeys(Request.KeysQuery(requestId, userIds))
            olmMachine.markRequestAsSent(requestId, RequestType.KEYS_QUERY, response)
        }
    }
}
