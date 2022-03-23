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
import org.amshove.kluent.shouldBeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.database.helper.addTimelineEvent
import org.matrix.android.sdk.internal.database.helper.merge
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.SessionRealmModule
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.session.room.timeline.RoomDataHelper.createFakeListOfEvents
import org.matrix.android.sdk.session.room.timeline.RoomDataHelper.createFakeMessageEvent

@RunWith(AndroidJUnit4::class)
internal class ChunkEntityTest : InstrumentedTest {

    private lateinit var monarchy: Monarchy

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

            val fakeEvent = createFakeMessageEvent().toEntity(ROOM_ID, SendState.SYNCED, System.currentTimeMillis()).let {
                realm.copyToRealm(it)
            }
            chunk.addTimelineEvent(
                    roomId = ROOM_ID,
                    eventEntity = fakeEvent,
                    direction = PaginationDirection.FORWARDS,
                    roomMemberContentsByUser = emptyMap())
            chunk.timelineEvents.size shouldBeEqualTo 1
        }
    }

    @Test
    fun add_shouldNotAdd_whenAlreadyIncluded() {
        monarchy.runTransactionSync { realm ->
            val chunk: ChunkEntity = realm.createObject()
            val fakeEvent = createFakeMessageEvent().toEntity(ROOM_ID, SendState.SYNCED, System.currentTimeMillis()).let {
                realm.copyToRealm(it)
            }
            chunk.addTimelineEvent(
                    roomId = ROOM_ID,
                    eventEntity = fakeEvent,
                    direction = PaginationDirection.FORWARDS,
                    roomMemberContentsByUser = emptyMap())
            chunk.addTimelineEvent(
                    roomId = ROOM_ID,
                    eventEntity = fakeEvent,
                    direction = PaginationDirection.FORWARDS,
                    roomMemberContentsByUser = emptyMap())
            chunk.timelineEvents.size shouldBeEqualTo 1
        }
    }

    @Test
    fun merge_shouldAddEvents_whenMergingBackward() {
        monarchy.runTransactionSync { realm ->
            val chunk1: ChunkEntity = realm.createObject()
            val chunk2: ChunkEntity = realm.createObject()
            chunk1.addAll(ROOM_ID, createFakeListOfEvents(30), PaginationDirection.BACKWARDS)
            chunk2.addAll(ROOM_ID, createFakeListOfEvents(30), PaginationDirection.BACKWARDS)
            chunk1.merge(ROOM_ID, chunk2, PaginationDirection.BACKWARDS)
            chunk1.timelineEvents.size shouldBeEqualTo 60
        }
    }

    @Test
    fun merge_shouldAddOnlyDifferentEvents_whenMergingBackward() {
        monarchy.runTransactionSync { realm ->
            val chunk1: ChunkEntity = realm.createObject()
            val chunk2: ChunkEntity = realm.createObject()
            val eventsForChunk1 = createFakeListOfEvents(30)
            val eventsForChunk2 = eventsForChunk1 + createFakeListOfEvents(10)
            chunk1.isLastForward = true
            chunk2.isLastForward = false
            chunk1.addAll(ROOM_ID, eventsForChunk1, PaginationDirection.FORWARDS)
            chunk2.addAll(ROOM_ID, eventsForChunk2, PaginationDirection.BACKWARDS)
            chunk1.merge(ROOM_ID, chunk2, PaginationDirection.BACKWARDS)
            chunk1.timelineEvents.size shouldBeEqualTo 40
            chunk1.isLastForward.shouldBeTrue()
        }
    }

    @Test
    fun merge_shouldPrevTokenMerged_whenMergingForwards() {
        monarchy.runTransactionSync { realm ->
            val chunk1: ChunkEntity = realm.createObject()
            val chunk2: ChunkEntity = realm.createObject()
            val prevToken = "prev_token"
            chunk1.prevToken = prevToken
            chunk1.addAll(ROOM_ID, createFakeListOfEvents(30), PaginationDirection.BACKWARDS)
            chunk2.addAll(ROOM_ID, createFakeListOfEvents(30), PaginationDirection.BACKWARDS)
            chunk1.merge(ROOM_ID, chunk2, PaginationDirection.FORWARDS)
            chunk1.prevToken shouldBeEqualTo prevToken
        }
    }

    @Test
    fun merge_shouldNextTokenMerged_whenMergingBackwards() {
        monarchy.runTransactionSync { realm ->
            val chunk1: ChunkEntity = realm.createObject()
            val chunk2: ChunkEntity = realm.createObject()
            val nextToken = "next_token"
            chunk1.nextToken = nextToken
            chunk1.addAll(ROOM_ID, createFakeListOfEvents(30), PaginationDirection.BACKWARDS)
            chunk2.addAll(ROOM_ID, createFakeListOfEvents(30), PaginationDirection.BACKWARDS)
            chunk1.merge(ROOM_ID, chunk2, PaginationDirection.BACKWARDS)
            chunk1.nextToken shouldBeEqualTo nextToken
        }
    }

    private fun ChunkEntity.addAll(roomId: String,
                                   events: List<Event>,
                                   direction: PaginationDirection) {
        events.forEach { event ->
            val fakeEvent = event.toEntity(roomId, SendState.SYNCED, System.currentTimeMillis()).let {
                realm.copyToRealm(it)
            }
            addTimelineEvent(
                    roomId = roomId,
                    eventEntity = fakeEvent,
                    direction = direction,
                    roomMemberContentsByUser = emptyMap())
        }
    }

    companion object {
        private const val ROOM_ID = "roomId"
    }
}
