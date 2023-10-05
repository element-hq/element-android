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

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.crypto.network.OutgoingRequestsProcessor
import org.matrix.rustcomponents.sdk.crypto.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

internal class SecretShareManager @Inject constructor(
        private val olmMachine: Provider<OlmMachine>,
        private val outgoingRequestsProcessor: OutgoingRequestsProcessor) {

    suspend fun requestSecretTo(deviceId: String, secretName: String) {
        Timber.w("SecretShareManager requesting custom secrets not supported $deviceId, $secretName")
        // rust stack only support requesting secrets defined in the spec (not custom secret yet)
        requestMissingSecrets()
    }

    suspend fun requestMissingSecrets() {
        this.olmMachine.get().requestMissingSecretsFromOtherSessions()

        // immediately send the requests
        outgoingRequestsProcessor.processOutgoingRequests(this.olmMachine.get()) {
            it is Request.ToDevice && it.eventType == EventType.REQUEST_SECRET
        }
    }
}
