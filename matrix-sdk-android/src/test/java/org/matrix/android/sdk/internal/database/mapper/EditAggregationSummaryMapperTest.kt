/*
 * Copyright (c) 2022 New Vector Ltd
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
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldNotBe
import org.junit.Test
import org.matrix.android.sdk.internal.database.model.EditAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.EditionOfEvent
import org.matrix.android.sdk.internal.database.model.EventEntity

class EditAggregationSummaryMapperTest {

    @Test
    fun test() {
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
        mapped!!.sourceEvents.size shouldBe 2
        mapped.localEchos.size shouldBe 1
        mapped.localEchos.first() shouldBe "e2"

        mapped.lastEditTs shouldBe 30L
        mapped.latestEdit?.eventId shouldBe "e2"
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
