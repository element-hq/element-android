/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.internal.crypto

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.internal.crypto.model.rest.SecretShareRequest

/**
 * IncomingRoomKeyRequest class defines the incoming room keys request.
 */
data class IncomingSecretShareRequest(
        /**
         * The user id
         */
        override val userId: String? = null,

        /**
         * The device id
         */
        override val deviceId: String? = null,

        /**
         * The request id
         */
        override val requestId: String? = null,

        /**
         * The request body
         */
        val secretName: String? = null,

        /**
         * The runnable to call to accept to share the keys
         */
        @Transient
        var share: ((String) -> Unit)? = null,

        /**
         * The runnable to call to ignore the key share request.
         */
        @Transient
        var ignore: Runnable? = null,

        override val localCreationTimestamp: Long?

) : IncomingShareRequestCommon {
    companion object {
        /**
         * Factory
         *
         * @param event the event
         */
        fun fromEvent(event: Event): IncomingSecretShareRequest? {
            return event.getClearContent()
                    .toModel<SecretShareRequest>()
                    ?.let {
                        IncomingSecretShareRequest(
                                userId = event.senderId,
                                deviceId = it.requestingDeviceId,
                                requestId = it.requestId,
                                secretName = it.secretName,
                                localCreationTimestamp = event.ageLocalTs ?: System.currentTimeMillis()
                        )
                    }
        }
    }
}
