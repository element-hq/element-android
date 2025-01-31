/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.tasks

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.internal.crypto.api.CryptoApi
import org.matrix.android.sdk.internal.crypto.model.rest.DeviceKeysWithUnsigned
import org.matrix.android.sdk.internal.crypto.model.rest.KeysQueryBody
import org.matrix.android.sdk.internal.crypto.model.rest.KeysQueryResponse
import org.matrix.android.sdk.internal.crypto.model.rest.RestKeyInfo
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.computeBestChunkSize
import javax.inject.Inject

internal interface DownloadKeysForUsersTask : Task<DownloadKeysForUsersTask.Params, KeysQueryResponse> {
    data class Params(
            // the list of users to get keys for. The list MUST NOT be empty
            val userIds: List<String>,
            // the up-to token
            val token: String?
    )
}

internal class DefaultDownloadKeysForUsers @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : DownloadKeysForUsersTask {

    override suspend fun execute(params: DownloadKeysForUsersTask.Params): KeysQueryResponse {
        val bestChunkSize = computeBestChunkSize(params.userIds.size, LIMIT)
        val token = params.token?.takeIf { token -> token.isNotEmpty() }

        return if (bestChunkSize.shouldChunk()) {
            // Store server results in these mutable maps
            val deviceKeys = mutableMapOf<String, Map<String, DeviceKeysWithUnsigned>>()
            val failures = mutableMapOf<String, Map<String, Any>>()
            val masterKeys = mutableMapOf<String, RestKeyInfo?>()
            val selfSigningKeys = mutableMapOf<String, RestKeyInfo?>()
            val userSigningKeys = mutableMapOf<String, RestKeyInfo?>()

            val mutex = Mutex()

            // Split network request into smaller request (#2925)
            coroutineScope {
                params.userIds
                        .chunked(bestChunkSize.chunkSize)
                        .map {
                            KeysQueryBody(
                                    deviceKeys = it.associateWith { emptyList() },
                                    token = token
                            )
                        }
                        .map { body ->
                            async {
                                val result = executeRequest(globalErrorReceiver) {
                                    cryptoApi.downloadKeysForUsers(body)
                                }

                                mutex.withLock {
                                    deviceKeys.putAll(result.deviceKeys.orEmpty())
                                    failures.putAll(result.failures.orEmpty())
                                    masterKeys.putAll(result.masterKeys.orEmpty())
                                    selfSigningKeys.putAll(result.selfSigningKeys.orEmpty())
                                    userSigningKeys.putAll(result.userSigningKeys.orEmpty())
                                }
                            }
                        }
                        .joinAll()
            }

            KeysQueryResponse(
                    deviceKeys = deviceKeys,
                    failures = failures,
                    masterKeys = masterKeys,
                    selfSigningKeys = selfSigningKeys,
                    userSigningKeys = userSigningKeys
            )
        } else {
            // No need to chunk, direct request
            executeRequest(globalErrorReceiver) {
                cryptoApi.downloadKeysForUsers(
                        KeysQueryBody(
                                deviceKeys = params.userIds.associateWith { emptyList() },
                                token = token
                        )
                )
            }
        }
    }

    companion object {
        const val LIMIT = 250
    }
}
