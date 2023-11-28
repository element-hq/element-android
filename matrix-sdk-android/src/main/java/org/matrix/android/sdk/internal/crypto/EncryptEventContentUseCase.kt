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

import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.model.MXEncryptEventContentResult
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.internal.util.time.Clock
import timber.log.Timber
import javax.inject.Inject

private val loggerTag = LoggerTag("EncryptEventContentUseCase", LoggerTag.CRYPTO)

internal class EncryptEventContentUseCase @Inject constructor(
        private val olmMachine: OlmMachine,
        private val prepareToEncrypt: PrepareToEncryptUseCase,
        private val clock: Clock) {

    suspend operator fun invoke(
            eventContent: Content,
            eventType: String,
            roomId: String): MXEncryptEventContentResult {
        val t0 = clock.epochMillis()

        /**
         * When using in-room messages and the room has encryption enabled,
         * clients should ensure that encryption does not hinder the verification.
         * For example, if the verification messages are encrypted, clients must ensure that all the recipient’s
         * unverified devices receive the keys necessary to decrypt the messages,
         * even if they would normally not be given the keys to decrypt messages in the room.
         */
        val shouldSendToUnverified = isVerificationEvent(eventType, eventContent)

        prepareToEncrypt(roomId, ensureAllMembersAreLoaded = false, forceDistributeToUnverified = shouldSendToUnverified)
        val content = olmMachine.encrypt(roomId, eventType, eventContent)
        Timber.tag(loggerTag.value).v("## CRYPTO | encryptEventContent() : succeeds after ${clock.epochMillis() - t0} ms")
        return MXEncryptEventContentResult(content, EventType.ENCRYPTED)
    }

    private fun isVerificationEvent(eventType: String, eventContent: Content) =
            EventType.isVerificationEvent(eventType) ||
                    (eventType == EventType.MESSAGE &&
                            eventContent.get(MessageContent.MSG_TYPE_JSON_KEY) == MessageType.MSGTYPE_VERIFICATION_REQUEST)
}
