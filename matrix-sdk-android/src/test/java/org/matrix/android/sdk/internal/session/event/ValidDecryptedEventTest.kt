/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.event

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldNotBe
import org.junit.Test
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.events.model.toValidDecryptedEvent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent

class ValidDecryptedEventTest {

    private val fakeEvent = Event(
            type = EventType.ENCRYPTED,
            eventId = "\$eventId",
            roomId = "!fakeRoom",
            content = EncryptedEventContent(
                    algorithm = MXCRYPTO_ALGORITHM_MEGOLM,
                    ciphertext = "AwgBEpACQEKOkd4Gp0+gSXG4M+btcrnPgsF23xs/lUmS2I4YjmqF...",
                    sessionId = "TO2G4u2HlnhtbIJk",
                    senderKey = "5e3EIqg3JfooZnLQ2qHIcBarbassQ4qXblai0",
                    deviceId = "FAKEE"
            ).toContent()
    )

    @Test
    fun `A failed to decrypt message should give a null validated decrypted event`() {
        fakeEvent.toValidDecryptedEvent() shouldBe null
    }

    @Test
    fun `Mismatch sender key detection`() {
        val decryptedEvent = fakeEvent
                .apply {
                    mxDecryptionResult = OlmDecryptionResult(
                            payload = mapOf(
                                    "type" to EventType.MESSAGE,
                                    "content" to mapOf(
                                            "body" to "some message",
                                            "msgtype" to "m.text"
                                    ),
                            ),
                            senderKey = "the_real_sender_key",
                    )
                }

        val validDecryptedEvent = decryptedEvent.toValidDecryptedEvent()
        validDecryptedEvent shouldNotBe null

        fakeEvent.content!!["senderKey"] shouldNotBe "the_real_sender_key"
        validDecryptedEvent!!.cryptoSenderKey shouldBe "the_real_sender_key"
    }

    @Test
    fun `Mixed content event should be detected`() {
        val mixedEvent = Event(
                type = EventType.ENCRYPTED,
                eventId = "\$eventd ",
                roomId = "!fakeRoo",
                content = mapOf(
                        "algorithm" to "m.megolm.v1.aes-sha2",
                        "ciphertext" to "AwgBEpACQEKOkd4Gp0+gSXG4M+btcrnPgsF23xs/lUmS2I4YjmqF...",
                        "sessionId" to "TO2G4u2HlnhtbIJk",
                        "senderKey" to "5e3EIqg3JfooZnLQ2qHIcBarbassQ4qXblai0",
                        "deviceId" to "FAKEE",
                        "body" to "some message",
                        "msgtype" to "m.text"
                ).toContent()
        )

        val unValidatedContent = mixedEvent.content.toModel<MessageTextContent>()
        unValidatedContent?.body shouldBe "some message"

        mixedEvent.toValidDecryptedEvent()?.clearContent?.toModel<MessageTextContent>() shouldBe null
    }

    @Test
    fun `Basic field validation`() {
        val decryptedEvent = fakeEvent
                .apply {
                    mxDecryptionResult = OlmDecryptionResult(
                            payload = mapOf(
                                    "type" to EventType.MESSAGE,
                                    "content" to mapOf(
                                            "body" to "some message",
                                            "msgtype" to "m.text"
                                    ),
                            ),
                            senderKey = "the_real_sender_key",
                    )
                }

        decryptedEvent.toValidDecryptedEvent() shouldNotBe null
        decryptedEvent.copy(roomId = null).toValidDecryptedEvent() shouldBe null
        decryptedEvent.copy(eventId = null).toValidDecryptedEvent() shouldBe null
    }

    @Test
    fun `A clear event is not a valid decrypted event`() {
        val mockTextEvent = Event(
                type = EventType.MESSAGE,
                eventId = "eventId",
                roomId = "!fooe:example.com",
                content = mapOf(
                        "body" to "some message",
                        "msgtype" to "m.text"
                ),
                originServerTs = 1000,
                senderId = "@anne:example.com",
        )
        mockTextEvent.toValidDecryptedEvent() shouldBe null
    }
}
