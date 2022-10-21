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
import io.realm.kotlin.MutableRealm
import io.realm.kotlin.RealmConfiguration
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.helper.addTimelineEvent
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.SESSION_REALM_SCHEMA
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.internal.util.time.DefaultClock
import org.matrix.android.sdk.session.room.timeline.RoomDataHelper.createFakeMessageEvent

@RunWith(AndroidJUnit4::class)
internal class ChunkEntityTest : InstrumentedTest {

    private lateinit var realmInstance: RealmInstance
    private val clock = DefaultClock()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        val testConfig = RealmConfiguration.Builder(SESSION_REALM_SCHEMA)
                //.inMemory()
                .name("test-realm")
                .build()
        realmInstance = RealmInstance(
                coroutineScope = TestScope(),
                realmConfiguration = testConfig,
                coroutineDispatcher = Main,
        )
    }

    @Test
    fun add_shouldAdd_whenNotAlreadyIncluded() {
        realmInstance.blockingWrite {
            val chunk = ChunkEntity()

            val fakeEvent = createFakeMessageEvent().toEntity(ROOM_ID, SendState.SYNCED, clock.epochMillis()).let {
                copyToRealm(it)
            }
            chunk.addTimelineEvent(
                    realm = this,
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
        realmInstance.blockingWrite {
            val chunk = ChunkEntity()
            val fakeEvent = createFakeMessageEvent().toEntity(ROOM_ID, SendState.SYNCED, clock.epochMillis()).let {
                copyToRealm(it)
            }
            chunk.addTimelineEvent(
                    realm = this,
                    roomId = ROOM_ID,
                    eventEntity = fakeEvent,
                    direction = PaginationDirection.FORWARDS,
                    roomMemberContentsByUser = emptyMap()
            )
            chunk.addTimelineEvent(
                    realm = this,
                    roomId = ROOM_ID,
                    eventEntity = fakeEvent,
                    direction = PaginationDirection.FORWARDS,
                    roomMemberContentsByUser = emptyMap()
            )
            chunk.timelineEvents.size shouldBeEqualTo 1
        }
    }

    private fun ChunkEntity.addAll(
            realm: MutableRealm,
            roomId: String,
            events: List<Event>,
            direction: PaginationDirection
    ) {
        events.forEach { event ->
            val fakeEvent = event.toEntity(roomId, SendState.SYNCED, clock.epochMillis()).let {
                realm.copyToRealm(it)
            }
            addTimelineEvent(
                    realm = realm,
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
