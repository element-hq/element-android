/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.alias

import org.matrix.android.sdk.api.MatrixPatterns.getDomain
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.room.alias.RoomAliasError
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.directory.DirectoryAPI
import javax.inject.Inject

internal class RoomAliasAvailabilityChecker @Inject constructor(
        @UserId private val userId: String,
        private val directoryAPI: DirectoryAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) {
    /**
     * @param aliasLocalPart the local part of the alias.
     * Ex: for the alias "#my_alias:example.org", the local part is "my_alias"
     */
    @Throws(RoomAliasError::class)
    suspend fun check(aliasLocalPart: String?) {
        if (aliasLocalPart.isNullOrEmpty()) {
            // don't check empty or not provided alias
            return
        }
        if (aliasLocalPart.isBlank()) {
            throw RoomAliasError.AliasIsBlank
        }
        // Check alias availability
        val fullAlias = aliasLocalPart.toFullLocalAlias(userId)
        try {
            executeRequest(globalErrorReceiver) {
                directoryAPI.getRoomIdByAlias(fullAlias)
            }
        } catch (throwable: Throwable) {
            if (throwable is Failure.ServerError && throwable.httpCode == 404) {
                // This is a 404, so the alias is available: nominal case
                return
            } else {
                // Other error, propagate it
                throw throwable
            }
        }
                .let {
                    // Alias already exists: error case
                    throw RoomAliasError.AliasNotAvailable
                }
    }

    companion object {
        internal fun String.toFullLocalAlias(userId: String) = "#" + this + ":" + userId.getDomain()
    }
}
