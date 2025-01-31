/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
