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

package org.matrix.android.sdk.internal.crypto

import org.amshove.kluent.shouldNotContain
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.sync.model.ToDeviceSyncResponse
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.network.parsing.CheckNumberType

class MoshiNumbersAsInt {

    @Test
    fun numberShouldNotPutAllAsFloat() {
        val event = Event(
                type = "m.room.encrypted",
                eventId = null,
                content = mapOf(
                        "algorithm" to "m.olm.v1.curve25519-aes-sha2",
                        "ciphertext" to mapOf(
                                "cfA3dINwtmMW0DbJmnT6NiGAbOSa299Hxs6KxHgbDBw" to mapOf(
                                        "body" to "Awogc5...",
                                        "type" to 1
                                ),
                        ),
                ),
                prevContent = null,
                originServerTs = null,
                senderId = "@web:localhost:8481"
        )

        val toDeviceSyncResponse = ToDeviceSyncResponse(listOf(event))

        val adapter = MoshiProvider.providesMoshi().adapter(ToDeviceSyncResponse::class.java)

        val jsonString = adapter.toJson(toDeviceSyncResponse)

        jsonString shouldNotContain "1.0"
    }

    @Test
    fun testParseThenSerialize() {
        val raw = """
            {"events":[{"type":"m.room.encrypted","content":{"algorithm":"m.olm.v1.curve25519-aes-sha2","ciphertext":{"cfA3dINwtmMW0DbJmnT6NiGAbOSa299Hxs6KxHgbDBw":{"body":"Awogc5L3QuIyvkluB1O/UAJp0","type":1}},"sender_key":"fqhBEOHXSSQ7ZKt1xlBg+hSTY1NEM8hezMXZ5lyBR1M"},"sender":"@web:localhost:8481"}]}
        """.trimIndent()

        val moshi = MoshiProvider.providesMoshi()
        val adapter = moshi.adapter(ToDeviceSyncResponse::class.java)

        val content = adapter.fromJson(raw)

        val serialized = MoshiProvider.providesMoshi()
                .newBuilder()
                .add(CheckNumberType.JSON_ADAPTER_FACTORY)
                .build()
                .adapter(ToDeviceSyncResponse::class.java).toJson(content)

        serialized shouldNotContain "1.0"

        println(serialized)
    }
}
