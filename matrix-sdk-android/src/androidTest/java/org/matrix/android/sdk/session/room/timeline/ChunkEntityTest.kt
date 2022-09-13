/*
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

package org.matrix.android.sdk.session.room.timeline

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.createObject
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.database.helper.addTimelineEvent
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.SessionRealmModule
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.internal.util.time.DefaultClock
import org.matrix.android.sdk.session.room.timeline.RoomDataHelper.createFakeMessageEvent

@RunWith(AndroidJUnit4::class)
internal class ChunkEntityTest : InstrumentedTest {

    private lateinit var monarchy: Monarchy
    private val clock = DefaultClock()

    @Before
    fun setup() {
        Realm.init(context())
        val testConfig = RealmConfiguration.Builder()
                .inMemory()
                .name("test-realm")
                .modules(SessionRealmModule())
                .build()
        monarchy = Monarchy.Builder().setRealmConfiguration(testConfig).build()
    }

    @Test
    fun add_shouldAdd_whenNotAlreadyIncluded() {
        monarchy.runTransactionSync { realm ->
            val chunk: ChunkEntity = realm.createObject()

            val fakeEvent = createFakeMessageEvent().toEntity(ROOM_ID, SendState.SYNCED, clock.epochMillis()).let {
                realm.copyToRealm(it)
            }
            chunk.addTimelineEvent(
                    roomId = ROOM_ID,
                    eventEntity = fakeEvent,
                    direction = PaginationDirection.FORWARDS,
                    roomMemberContentsByUser = emptyMap()
            )
            chunk.timelineEvents.size shouldBeEqualTo 1
        }
    }

    @Test
    fun add_shouldNotAdd_whenAlreadyIncluded() {
        monarchy.runTransactionSync { realm ->
            val chunk: ChunkEntity = realm.createObject()
            val fakeEvent = createFakeMessageEvent().toEntity(ROOM_ID, SendState.SYNCED, clock.epochMillis()).let {
                realm.copyToRealm(it)
            }
            chunk.addTimelineEvent(
                    roomId = ROOM_ID,
                    eventEntity = fakeEvent,
                    direction = PaginationDirection.FORWARDS,
                    roomMemberContentsByUser = emptyMap()
            )
            chunk.addTimelineEvent(
                    roomId = ROOM_ID,
                    eventEntity = fakeEvent,
                    direction = PaginationDirection.FORWARDS,
                    roomMemberContentsByUser = emptyMap()
            )
            chunk.timelineEvents.size shouldBeEqualTo 1
        }
    }

    private fun ChunkEntity.addAll(
            roomId: String,
            events: List<Event>,
            direction: PaginationDirection
    ) {
        events.forEach { event ->
            val fakeEvent = event.toEntity(roomId, SendState.SYNCED, clock.epochMillis()).let {
                realm.copyToRealm(it)
            }
            addTimelineEvent(
                    roomId = roomId,
                    eventEntity = fakeEvent,
                    direction = direction,
                    roomMemberContentsByUser = emptyMap()
            )
        }
    }

    companion object {
        private const val ROOM_ID = "roomId"
    }
}
