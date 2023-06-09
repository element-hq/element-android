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

package org.matrix.android.sdk.internal.session.room

import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.crypto.store.IMXCommonCryptoStore

class EventEditValidatorTest {

    private val mockTextEvent = Event(
            type = EventType.MESSAGE,
            eventId = "\$WX8WlNC2reiXrwHIA_CQHmU_pSR-jhOA2xKPRcJN9wQ",
            roomId = "!GXKhWsRwiWWvbQDBpe:example.com",
            content = mapOf(
                    "body" to "some message",
                    "msgtype" to "m.text"
            ),
            originServerTs = 1000,
            senderId = "@alice:example.com",
    )

    private val mockEdit = Event(
            type = EventType.MESSAGE,
            eventId = "\$-SF7RWLPzRzCbHqK3ZAhIrX5Auh3B2lS5AqJiypt1p0",
            roomId = "!GXKhWsRwiWWvbQDBpe:example.com",
            content = mapOf(
                    "body" to "* some message edited",
                    "msgtype" to "m.text",
                    "m.new_content" to mapOf(
                            "body" to "some message edited",
                            "msgtype" to "m.text"
                    ),
                    "m.relates_to" to mapOf(
                            "rel_type" to "m.replace",
                            "event_id" to mockTextEvent.eventId
                    )
            ),
            originServerTs = 2000,
            senderId = "@alice:example.com",
    )

    @Test
    fun `edit should be valid`() {
        val mockCryptoStore = mockk<IMXCommonCryptoStore>()
        val validator = EventEditValidator(mockCryptoStore)

        validator
                .validateEdit(mockTextEvent, mockEdit) shouldBeInstanceOf EventEditValidator.EditValidity.Valid::class
    }

    @Test
    fun `original event and replacement event must have the same sender`() {
        val mockCryptoStore = mockk<IMXCommonCryptoStore>()
        val validator = EventEditValidator(mockCryptoStore)

        validator
                .validateEdit(
                        mockTextEvent,
                        mockEdit.copy(senderId = "@bob:example.com")
                ) shouldBeInstanceOf EventEditValidator.EditValidity.Invalid::class
    }

    @Test
    fun `original event and replacement event must have the same room_id`() {
        val mockCryptoStore = mockk<IMXCommonCryptoStore>()
        val validator = EventEditValidator(mockCryptoStore)

        validator
                .validateEdit(
                        mockTextEvent,
                        mockEdit.copy(roomId = "!someotherroom")
                ) shouldBeInstanceOf EventEditValidator.EditValidity.Invalid::class

        validator
                .validateEdit(
                        encryptedEvent,
                        encryptedEditEvent.copy(roomId = "!someotherroom")
                ) shouldBeInstanceOf EventEditValidator.EditValidity.Invalid::class
    }

    @Test
    fun `replacement and original events must not have a state_key property`() {
        val mockCryptoStore = mockk<IMXCommonCryptoStore>()
        val validator = EventEditValidator(mockCryptoStore)

        validator
                .validateEdit(
                        mockTextEvent,
                        mockEdit.copy(stateKey = "")
                ) shouldBeInstanceOf EventEditValidator.EditValidity.Invalid::class

        validator
                .validateEdit(
                        mockTextEvent.copy(stateKey = ""),
                        mockEdit
                ) shouldBeInstanceOf EventEditValidator.EditValidity.Invalid::class
    }

    @Test
    fun `replacement event must have an new_content property`() {
        val mockCryptoStore = mockk<IMXCommonCryptoStore> {
            every { deviceWithIdentityKey("@alice:example.com", "R0s/7Aindgg/RNWqUGJyJOXtCz5H7Gx7fInFuroq1xo") } returns
                    mockk<CryptoDeviceInfo> {
                        every { userId } returns "@alice:example.com"
                    }
        }
        val validator = EventEditValidator(mockCryptoStore)

        validator
                .validateEdit(mockTextEvent, mockEdit.copy(
                        content = mockEdit.content!!.toMutableMap().apply {
                            this.remove("m.new_content")
                        }
                )) shouldBeInstanceOf EventEditValidator.EditValidity.Invalid::class

        validator
                .validateEdit(
                        encryptedEvent,
                        encryptedEditEvent.copy().apply {
                            mxDecryptionResult = encryptedEditEvent.mxDecryptionResult!!.copy(
                                    payload = mapOf(
                                            "type" to EventType.MESSAGE,
                                            "content" to mapOf(
                                                    "body" to "* some message edited",
                                                    "msgtype" to "m.text",
                                                    "m.relates_to" to mapOf(
                                                            "rel_type" to "m.replace",
                                                            "event_id" to mockTextEvent.eventId
                                                    )
                                            )
                                    )
                            )
                        }
                ) shouldBeInstanceOf EventEditValidator.EditValidity.Invalid::class
    }

    @Test
    fun `The original event must not itself have a rel_type of m_replace`() {
        val mockCryptoStore = mockk<IMXCommonCryptoStore> {
            every { deviceWithIdentityKey("@alice:example.com", "R0s/7Aindgg/RNWqUGJyJOXtCz5H7Gx7fInFuroq1xo") } returns
                    mockk<CryptoDeviceInfo> {
                        every { userId } returns "@alice:example.com"
                    }
        }
        val validator = EventEditValidator(mockCryptoStore)

        validator
                .validateEdit(
                        mockTextEvent.copy(
                                content = mockTextEvent.content!!.toMutableMap().apply {
                                    this["m.relates_to"] = mapOf(
                                            "rel_type" to "m.replace",
                                            "event_id" to mockTextEvent.eventId
                                    )
                                }
                        ),
                        mockEdit
                ) shouldBeInstanceOf EventEditValidator.EditValidity.Invalid::class

        validator
                .validateEdit(
                        encryptedEvent.copy(
                                content = encryptedEvent.content!!.toMutableMap().apply {
                                    put(
                                            "m.relates_to",
                                            mapOf(
                                                    "rel_type" to "m.replace",
                                                    "event_id" to mockTextEvent.eventId
                                            )
                                    )
                                }
                        ).apply {
                            mxDecryptionResult = encryptedEditEvent.mxDecryptionResult!!.copy(
                                    payload = mapOf(
                                            "type" to EventType.MESSAGE,
                                            "content" to mapOf(
                                                    "body" to "some message",
                                                    "msgtype" to "m.text",
                                            ),
                                    )
                            )
                        },
                        encryptedEditEvent
                ) shouldBeInstanceOf EventEditValidator.EditValidity.Invalid::class
    }

    @Test
    fun `valid e2ee edit`() {
        val mockCryptoStore = mockk<IMXCommonCryptoStore> {
            every { deviceWithIdentityKey("@alice:example.com", "R0s/7Aindgg/RNWqUGJyJOXtCz5H7Gx7fInFuroq1xo") } returns
                    mockk<CryptoDeviceInfo> {
                        every { userId } returns "@alice:example.com"
                    }
        }
        val validator = EventEditValidator(mockCryptoStore)

        validator
                .validateEdit(
                        encryptedEvent,
                        encryptedEditEvent
                ) shouldBeInstanceOf EventEditValidator.EditValidity.Valid::class
    }

    @Test
    fun `If the original event was encrypted, the replacement should be too`() {
        val mockCryptoStore = mockk<IMXCommonCryptoStore> {
            every { deviceWithIdentityKey("@alice:example.com", "R0s/7Aindgg/RNWqUGJyJOXtCz5H7Gx7fInFuroq1xo") } returns
                    mockk<CryptoDeviceInfo> {
                        every { userId } returns "@alice:example.com"
                    }
        }
        val validator = EventEditValidator(mockCryptoStore)

        validator
                .validateEdit(
                        encryptedEvent,
                        mockEdit
                ) shouldBeInstanceOf EventEditValidator.EditValidity.Invalid::class
    }

    @Test
    fun `encrypted, original event and replacement event must have the same sender`() {
        val mockCryptoStore = mockk<IMXCommonCryptoStore> {
            every { deviceWithIdentityKey("@alice:example.com", "R0s/7Aindgg/RNWqUGJyJOXtCz5H7Gx7fInFuroq1xo") } returns
                    mockk {
                        every { userId } returns "@alice:example.com"
                    }
            every { deviceWithIdentityKey("@bob:example.com", "7V5e/2O93mf4GeW7Mtq4YWcRNpYS9NhQbdJMgdnIPUI") } returns
                    mockk {
                        every { userId } returns "@bob:example.com"
                    }
        }
        val validator = EventEditValidator(mockCryptoStore)

        validator
                .validateEdit(
                        encryptedEvent,
                        encryptedEditEvent.copy(
                                senderId = "@bob:example.com"
                        ).apply {
                            mxDecryptionResult = encryptedEditEvent.mxDecryptionResult!!.copy(
                                    senderKey = "7V5e/2O93mf4GeW7Mtq4YWcRNpYS9NhQbdJMgdnIPUI"
                            )
                        }

                ) shouldBeInstanceOf EventEditValidator.EditValidity.Invalid::class

        // if sent fom a deleted device it should use the event claimed sender id
    }

    @Test
    fun `encrypted, sent fom a deleted device, original event and replacement event must have the same sender`() {
        val mockCryptoStore = mockk<IMXCommonCryptoStore> {
            every { deviceWithIdentityKey("@alice:example.com", "R0s/7Aindgg/RNWqUGJyJOXtCz5H7Gx7fInFuroq1xo") } returns
                    mockk {
                        every { userId } returns "@alice:example.com"
                    }
            every { deviceWithIdentityKey(any(), "7V5e/2O93mf4GeW7Mtq4YWcRNpYS9NhQbdJMgdnIPUI") } returns
                    null
        }
        val validator = EventEditValidator(mockCryptoStore)

        validator
                .validateEdit(
                        encryptedEvent,
                        encryptedEditEvent.copy().apply {
                            mxDecryptionResult = encryptedEditEvent.mxDecryptionResult!!.copy(
                                    senderKey = "7V5e/2O93mf4GeW7Mtq4YWcRNpYS9NhQbdJMgdnIPUI"
                            )
                        }

                ) shouldBeInstanceOf EventEditValidator.EditValidity.Unknown::class

        validator
                .validateEdit(
                        encryptedEvent,
                        encryptedEditEvent.copy(
                                senderId = "bob@example.com"
                        ).apply {
                            mxDecryptionResult = encryptedEditEvent.mxDecryptionResult!!.copy(
                                    senderKey = "7V5e/2O93mf4GeW7Mtq4YWcRNpYS9NhQbdJMgdnIPUI"
                            )
                        }

                ) shouldBeInstanceOf EventEditValidator.EditValidity.Invalid::class
    }

    private val encryptedEditEvent = Event(
            type = EventType.ENCRYPTED,
            eventId = "\$-SF7RWLPzRzCbHqK3ZAhIrX5Auh3B2lS5AqJiypt1p0",
            roomId = "!GXKhWsRwiWWvbQDBpe:example.com",
            content = mapOf(
                    "algorithm" to "m.megolm.v1.aes-sha2",
                    "sender_key" to "R0s/7Aindgg/RNWqUGJyJOXtCz5H7Gx7fInFuroq1xo",
                    "session_id" to "7tOd6xon/R2zJpy2LlSKcKWIek2jvkim0sNdnZZCWMQ",
                    "device_id" to "QDHBLWOTSN",
                    "ciphertext" to "AwgXErAC6TgQ4bV6NFldlffTWuUV1gsBYH6JLQMqG...deLfCQOSPunSSNDFdWuDkB8Cg",
                    "m.relates_to" to mapOf(
                            "rel_type" to "m.replace",
                            "event_id" to mockTextEvent.eventId
                    )
            ),
            originServerTs = 2000,
            senderId = "@alice:example.com",
    ).apply {
        mxDecryptionResult = OlmDecryptionResult(
                payload = mapOf(
                        "type" to EventType.MESSAGE,
                        "content" to mapOf(
                                "body" to "* some message edited",
                                "msgtype" to "m.text",
                                "m.new_content" to mapOf(
                                        "body" to "some message edited",
                                        "msgtype" to "m.text"
                                ),
                                "m.relates_to" to mapOf(
                                        "rel_type" to "m.replace",
                                        "event_id" to mockTextEvent.eventId
                                )
                        )
                ),
                senderKey = "R0s/7Aindgg/RNWqUGJyJOXtCz5H7Gx7fInFuroq1xo",
                isSafe = true
        )
    }

    private val encryptedEvent = Event(
            type = EventType.ENCRYPTED,
            eventId = "\$WX8WlNC2reiXrwHIA_CQHmU_pSR-jhOA2xKPRcJN9wQ",
            roomId = "!GXKhWsRwiWWvbQDBpe:example.com",
            content = mapOf(
                    "algorithm" to "m.megolm.v1.aes-sha2",
                    "sender_key" to "R0s/7Aindgg/RNWqUGJyJOXtCz5H7Gx7fInFuroq1xo",
                    "session_id" to "7tOd6xon/R2zJpy2LlSKcKWIek2jvkim0sNdnZZCWMQ",
                    "device_id" to "QDHBLWOTSN",
                    "ciphertext" to "AwgXErAC6TgQ4bV6NFldlffTWuUV1gsBYH6JLQMqG+4Vr...Yf0gYyhVWZY4SedF3fTMwkjmTuel4fwrmq",
            ),
            originServerTs = 2000,
            senderId = "@alice:example.com",
    ).apply {
        mxDecryptionResult = OlmDecryptionResult(
                payload = mapOf(
                        "type" to EventType.MESSAGE,
                        "content" to mapOf(
                                "body" to "some message",
                                "msgtype" to "m.text"
                        ),
                ),
                senderKey = "R0s/7Aindgg/RNWqUGJyJOXtCz5H7Gx7fInFuroq1xo",
                isSafe = true
        )
    }
}
