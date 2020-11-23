/*
 * Copyright (c) 2020 New Vector Ltd
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

import org.greenrobot.eventbus.EventBus
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.room.alias.RoomAliasError
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.directory.DirectoryAPI
import javax.inject.Inject

internal class RoomAliasAvailabilityChecker @Inject constructor(
        @UserId private val userId: String,
        private val directoryAPI: DirectoryAPI,
        private val eventBus: EventBus
) {
    @Throws(RoomAliasError::class)
    suspend fun check(aliasLocalPart: String?) {
        if (aliasLocalPart.isNullOrEmpty()) {
            throw RoomAliasError.AliasEmpty
        }
        // Check alias availability
        val fullAlias = aliasLocalPart.toFullAlias(userId)
        try {
            executeRequest<RoomAliasDescription>(eventBus) {
                apiCall = directoryAPI.getRoomIdByAlias(fullAlias)
            }
        } catch (throwable: Throwable) {
            if (throwable is Failure.ServerError && throwable.httpCode == 404) {
                // This is a 404, so the alias is available: nominal case
                null
            } else {
                // Other error, propagate it
                throw throwable
            }
        }
                ?.let {
                    // Alias already exists: error case
                    throw RoomAliasError.AliasNotAvailable
                }
    }

    companion object {
        internal fun String.toFullAlias(userId: String) = "#" + this + ":" + userId.substringAfter(":")
    }
}
