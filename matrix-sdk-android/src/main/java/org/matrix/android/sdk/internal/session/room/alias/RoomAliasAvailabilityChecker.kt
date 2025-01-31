/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.alias

import org.matrix.android.sdk.api.MatrixPatterns.getServerName
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
        internal fun String.toFullLocalAlias(userId: String) = "#" + this + ":" + userId.getServerName()
    }
}
