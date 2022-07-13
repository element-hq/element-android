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

package org.matrix.android.sdk.internal.session.room.timeline

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.executeTransactionAwait
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBe
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntityFields
import org.matrix.android.sdk.internal.database.model.SessionRealmModule
import org.matrix.android.sdk.internal.session.room.timeline.fakes.FakeLightweightSettingsStorage
import org.matrix.android.sdk.internal.session.room.timeline.fakes.FakeStreamEventsManager
import org.matrix.android.sdk.internal.session.room.timeline.fakes.FakeTokenChunkEvent
import org.matrix.android.sdk.internal.session.room.timeline.fixtures.aListOfTextMessageEvents
import org.matrix.android.sdk.internal.util.time.DefaultClock

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class TokenChunkEventPersistorTest : InstrumentedTest {

    private lateinit var monarchy: Monarchy
    private lateinit var realm: Realm
    private val clock = DefaultClock()
    private val userId = "@userId:server.com"
    private val lightweightSettingsStorage = FakeLightweightSettingsStorage()
    private val streamEventsManager = FakeStreamEventsManager().apply {
        givenDispatchPaginatedEventReceived()
    }

    @Before
    fun setup() {
        Realm.init(context())
        val testConfig = RealmConfiguration.Builder()
                .inMemory()
                .name("TokenChunkEventPersistorTest")
                .modules(SessionRealmModule())
                .build()
        monarchy = Monarchy.Builder().setRealmConfiguration(testConfig).build()
        realm = Realm.getInstance(monarchy.realmConfiguration)
    }

    @After
    fun tearDown() {
        realm.close()
    }

    @Test
    fun given_new_chunk_events_should_all_be_added_and_unique() = runBlocking {
        val roomId = "room-id"
        val tokenChunkEventPersistor =
                TokenChunkEventPersistor(monarchy, userId, lightweightSettingsStorage, liveEventManager = { streamEventsManager.instance }, clock)
        val fakeTokenChunkEvent = FakeTokenChunkEvent(
                start = "start",
                end = "end",
                events = aListOfTextMessageEvents(10, roomId = roomId)
        )
        val result = tokenChunkEventPersistor.insertInDb(fakeTokenChunkEvent, roomId, direction = PaginationDirection.BACKWARDS)
        result shouldBeEqualTo TokenChunkEventPersistor.Result.SUCCESS
        realm.refresh()
        val chunkEntity = realm.where(ChunkEntity::class.java)
                .equalTo(ChunkEntityFields.NEXT_TOKEN, "start")
                .equalTo(ChunkEntityFields.PREV_TOKEN, "end")
                .findFirst()!!
        chunkEntity.nextChunk shouldBe null
        chunkEntity.prevChunk shouldBe null
        chunkEntity.timelineEvents.size shouldBeEqualTo 10
        Unit
    }

}
