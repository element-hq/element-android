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

package org.matrix.android.sdk.internal.session.room.create

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.realm.kotlin.where
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.tombstone.RoomTombstoneContent
import org.matrix.android.sdk.internal.database.awaitNotEmptyResult
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.util.time.DefaultClock
import org.matrix.android.sdk.test.fakes.FakeMonarchy
import org.matrix.android.sdk.test.fakes.FakeStateEventDataSource

private const val A_LOCAL_ROOM_ID = "local.a-local-room-id"
private const val AN_EXISTING_ROOM_ID = "an-existing-room-id"
private const val A_ROOM_ID = "a-room-id"
private const val MY_USER_ID = "my-user-id"

@ExperimentalCoroutinesApi
internal class DefaultCreateRoomFromLocalRoomTaskTest {

    private val fakeMonarchy = FakeMonarchy()
    private val clock = DefaultClock()
    private val createRoomTask = mockk<CreateRoomTask>()
    private val fakeStateEventDataSource = FakeStateEventDataSource()

    private val defaultCreateRoomFromLocalRoomTask = DefaultCreateRoomFromLocalRoomTask(
            userId = MY_USER_ID,
            monarchy = fakeMonarchy.instance,
            createRoomTask = createRoomTask,
            stateEventDataSource = fakeStateEventDataSource.instance,
            clock = clock
    )

    @Before
    fun setup() {
        mockkStatic("org.matrix.android.sdk.internal.database.RealmQueryLatchKt")
        coJustRun { awaitNotEmptyResult<Any>(realmConfiguration = any(), timeoutMillis = any(), builder = any()) }

        mockkStatic("org.matrix.android.sdk.internal.database.query.EventEntityQueriesKt")
        coEvery { any<EventEntity>().copyToRealmOrIgnore(fakeMonarchy.fakeRealm.instance, any()) } answers { firstArg() }

        mockkStatic("org.matrix.android.sdk.internal.database.query.CurrentStateEventEntityQueriesKt")
        every { CurrentStateEventEntity.getOrCreate(fakeMonarchy.fakeRealm.instance, any(), any(), any()) } answers {
            CurrentStateEventEntity(roomId = arg(2), stateKey = arg(3), type = arg(4))
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a local room id when execute then the existing room id is kept`() = runTest {
        // Given
        givenATombstoneEvent(
                Event(
                        roomId = A_LOCAL_ROOM_ID,
                        type = EventType.STATE_ROOM_TOMBSTONE,
                        stateKey = "",
                        content = RoomTombstoneContent(replacementRoomId = AN_EXISTING_ROOM_ID).toContent()
                )
        )

        // When
        val params = CreateRoomFromLocalRoomTask.Params(A_LOCAL_ROOM_ID)
        val result = defaultCreateRoomFromLocalRoomTask.execute(params)

        // Then
        verifyTombstoneEvent(AN_EXISTING_ROOM_ID)
        result shouldBeEqualTo AN_EXISTING_ROOM_ID
    }

    @Test
    fun `given a local room id when execute then it is correctly executed`() = runTest {
        // Given
        val aCreateRoomParams = mockk<CreateRoomParams>()
        val aLocalRoomSummaryEntity = mockk<LocalRoomSummaryEntity> {
            every { roomSummaryEntity } returns mockk(relaxed = true)
            every { createRoomParams } returns aCreateRoomParams
        }
        givenATombstoneEvent(null)
        givenALocalRoomSummaryEntity(aLocalRoomSummaryEntity)

        coEvery { createRoomTask.execute(any()) } returns A_ROOM_ID

        // When
        val params = CreateRoomFromLocalRoomTask.Params(A_LOCAL_ROOM_ID)
        val result = defaultCreateRoomFromLocalRoomTask.execute(params)

        // Then
        verifyTombstoneEvent(null)
        // CreateRoomTask has been called with the initial CreateRoomParams
        coVerify { createRoomTask.execute(aCreateRoomParams) }
        // The resulting roomId matches the roomId returned by the createRoomTask
        result shouldBeEqualTo A_ROOM_ID
        // A tombstone state event has been created
        coVerify { CurrentStateEventEntity.getOrCreate(realm = any(), roomId = A_LOCAL_ROOM_ID, stateKey = any(), type = EventType.STATE_ROOM_TOMBSTONE) }
    }

    private fun givenATombstoneEvent(event: Event?) {
        fakeStateEventDataSource.givenGetStateEventReturns(event)
    }

    private fun givenALocalRoomSummaryEntity(localRoomSummaryEntity: LocalRoomSummaryEntity) {
        every {
            fakeMonarchy.fakeRealm.instance
                    .where<LocalRoomSummaryEntity>()
                    .equalTo(LocalRoomSummaryEntityFields.ROOM_ID, A_LOCAL_ROOM_ID)
                    .findFirst()
        } returns localRoomSummaryEntity
    }

    private fun verifyTombstoneEvent(expectedRoomId: String?) {
        fakeStateEventDataSource.verifyGetStateEvent(A_LOCAL_ROOM_ID, EventType.STATE_ROOM_TOMBSTONE, QueryStringValue.IsEmpty)
        fakeStateEventDataSource.instance.getStateEvent(A_LOCAL_ROOM_ID, EventType.STATE_ROOM_TOMBSTONE, QueryStringValue.IsEmpty)
                ?.content.toModel<RoomTombstoneContent>()
                ?.replacementRoomId shouldBeEqualTo expectedRoomId
    }
}
