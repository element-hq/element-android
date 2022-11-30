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

package org.matrix.android.sdk.internal.database.mapper

import io.mockk.every
import io.mockk.mockk
import io.realm.RealmList
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBe
import org.junit.Test
import org.matrix.android.sdk.internal.database.model.EditAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.EditionOfEvent
import org.matrix.android.sdk.internal.database.model.EventEntity

class EditAggregatedSummaryEntityMapperTest {

    @Test
    fun `test mapping summary entity to model`() {
        val edits = RealmList<EditionOfEvent>(
                EditionOfEvent(
                        timestamp = 0L,
                        eventId = "e0",
                        isLocalEcho = false,
                        event = mockEvent("e0")
                ),
                EditionOfEvent(
                        timestamp = 1L,
                        eventId = "e1",
                        isLocalEcho = false,
                        event = mockEvent("e1")
                ),
                EditionOfEvent(
                        timestamp = 30L,
                        eventId = "e2",
                        isLocalEcho = true,
                        event = mockEvent("e2")
                )
        )
        val fakeSummaryEntity = mockk<EditAggregatedSummaryEntity> {
            every { editions } returns edits
        }

        val mapped = EditAggregatedSummaryEntityMapper.map(fakeSummaryEntity)
        mapped shouldNotBe null
        mapped!!.sourceEvents.size shouldBeEqualTo 2
        mapped.localEchos.size shouldBeEqualTo 1
        mapped.localEchos.first() shouldBeEqualTo "e2"

        mapped.lastEditTs shouldBeEqualTo 30L
        mapped.latestEdit?.eventId shouldBeEqualTo "e2"
    }

    @Test
    fun `event with lexicographically largest event_id is treated as more recent`() {
        val lowerId = "\$Albatross"
        val higherId = "\$Zebra"

        (higherId > lowerId) shouldBeEqualTo true
        val timestamp = 1669288766745L
        val edits = RealmList<EditionOfEvent>(
                EditionOfEvent(
                        timestamp = timestamp,
                        eventId = lowerId,
                        isLocalEcho = false,
                        event = mockEvent(lowerId)
                ),
                EditionOfEvent(
                        timestamp = timestamp,
                        eventId = higherId,
                        isLocalEcho = false,
                        event = mockEvent(higherId)
                ),
                EditionOfEvent(
                        timestamp = 1L,
                        eventId = "e2",
                        isLocalEcho = true,
                        event = mockEvent("e2")
                )
        )

        val fakeSummaryEntity = mockk<EditAggregatedSummaryEntity> {
            every { editions } returns edits
        }
        val mapped = EditAggregatedSummaryEntityMapper.map(fakeSummaryEntity)
        mapped!!.lastEditTs shouldBeEqualTo timestamp
        mapped.latestEdit?.eventId shouldBeEqualTo higherId
    }

    private fun mockEvent(eventId: String): EventEntity {
        return EventEntity().apply {
            this.eventId = eventId
            this.content = """
                {
                    "body" : "Hello",
                    "msgtype": "text"
                }
            """.trimIndent()
        }
    }
}
