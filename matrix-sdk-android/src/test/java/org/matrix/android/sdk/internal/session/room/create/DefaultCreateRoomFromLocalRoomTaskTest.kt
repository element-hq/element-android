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
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import io.realm.kotlin.where
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.room.model.LocalRoomCreationState
import org.matrix.android.sdk.api.session.room.model.LocalRoomSummary
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.internal.database.awaitNotEmptyResult
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.test.fakes.FakeMonarchy
import org.matrix.android.sdk.test.fakes.FakeRoomSummaryDataSource

private const val A_LOCAL_ROOM_ID = "local.a-local-room-id"
private const val AN_EXISTING_ROOM_ID = "an-existing-room-id"
private const val A_ROOM_ID = "a-room-id"

@ExperimentalCoroutinesApi
internal class DefaultCreateRoomFromLocalRoomTaskTest {

    private val fakeMonarchy = FakeMonarchy()
    private val createRoomTask = mockk<CreateRoomTask>()
    private val fakeRoomSummaryDataSource = FakeRoomSummaryDataSource()

    private val defaultCreateRoomFromLocalRoomTask = DefaultCreateRoomFromLocalRoomTask(
            monarchy = fakeMonarchy.instance,
            createRoomTask = createRoomTask,
            roomSummaryDataSource = fakeRoomSummaryDataSource.instance,
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
        val aCreateRoomParams = mockk<CreateRoomParams>(relaxed = true)
        givenALocalRoomSummary(aCreateRoomParams = aCreateRoomParams, aCreationState = LocalRoomCreationState.CREATED, aReplacementRoomId = AN_EXISTING_ROOM_ID)
        val aLocalRoomSummaryEntity = givenALocalRoomSummaryEntity(
                aCreateRoomParams = aCreateRoomParams,
                aCreationState = LocalRoomCreationState.CREATED,
                aReplacementRoomId = AN_EXISTING_ROOM_ID
        )

        // When
        val params = CreateRoomFromLocalRoomTask.Params(A_LOCAL_ROOM_ID)
        val result = defaultCreateRoomFromLocalRoomTask.execute(params)

        // Then
        fakeRoomSummaryDataSource.verifyGetLocalRoomSummary(A_LOCAL_ROOM_ID)
        result shouldBeEqualTo AN_EXISTING_ROOM_ID
        aLocalRoomSummaryEntity.replacementRoomId shouldBeEqualTo AN_EXISTING_ROOM_ID
        aLocalRoomSummaryEntity.creationState shouldBeEqualTo LocalRoomCreationState.CREATED
    }

    @Test
    fun `given a local room id when execute then it is correctly executed`() = runTest {
        // Given
        val aCreateRoomParams = mockk<CreateRoomParams>(relaxed = true)
        givenALocalRoomSummary(aCreateRoomParams = aCreateRoomParams, aReplacementRoomId = null)
        val aLocalRoomSummaryEntity = givenALocalRoomSummaryEntity(aCreateRoomParams = aCreateRoomParams, aReplacementRoomId = null)

        coEvery { createRoomTask.execute(any()) } returns A_ROOM_ID

        // When
        val params = CreateRoomFromLocalRoomTask.Params(A_LOCAL_ROOM_ID)
        val result = defaultCreateRoomFromLocalRoomTask.execute(params)

        // Then
        fakeRoomSummaryDataSource.verifyGetLocalRoomSummary(A_LOCAL_ROOM_ID)
        // CreateRoomTask has been called with the initial CreateRoomParams
        coVerify { createRoomTask.execute(aCreateRoomParams) }
        // The resulting roomId matches the roomId returned by the createRoomTask
        result shouldBeEqualTo A_ROOM_ID
        // The room creation state has correctly been updated
        verifyOrder {
            aLocalRoomSummaryEntity.creationState = LocalRoomCreationState.CREATING
            aLocalRoomSummaryEntity.creationState = LocalRoomCreationState.CREATED
        }
        // The local room summary has been updated with the created room id
        verify { aLocalRoomSummaryEntity.replacementRoomId = A_ROOM_ID }
        aLocalRoomSummaryEntity.replacementRoomId shouldBeEqualTo A_ROOM_ID
        aLocalRoomSummaryEntity.creationState shouldBeEqualTo LocalRoomCreationState.CREATED
    }

    @Test
    fun `given a local room id when execute with an exception then the creation state is correctly updated`() = runTest {
        // Given
        val aCreateRoomParams = mockk<CreateRoomParams>(relaxed = true)
        givenALocalRoomSummary(aCreateRoomParams = aCreateRoomParams, aReplacementRoomId = null)
        val aLocalRoomSummaryEntity = givenALocalRoomSummaryEntity(aCreateRoomParams = aCreateRoomParams, aReplacementRoomId = null)

        coEvery { createRoomTask.execute(any()) }.throws(mockk())

        // When
        val params = CreateRoomFromLocalRoomTask.Params(A_LOCAL_ROOM_ID)
        tryOrNull { defaultCreateRoomFromLocalRoomTask.execute(params) }

        // Then
        fakeRoomSummaryDataSource.verifyGetLocalRoomSummary(A_LOCAL_ROOM_ID)
        // CreateRoomTask has been called with the initial CreateRoomParams
        coVerify { createRoomTask.execute(aCreateRoomParams) }
        // The room creation state has correctly been updated
        verifyOrder {
            aLocalRoomSummaryEntity.creationState = LocalRoomCreationState.CREATING
            aLocalRoomSummaryEntity.creationState = LocalRoomCreationState.FAILURE
        }
        // The local room summary has been updated with the created room id
        aLocalRoomSummaryEntity.replacementRoomId.shouldBeNull()
        aLocalRoomSummaryEntity.creationState shouldBeEqualTo LocalRoomCreationState.FAILURE
    }

    private fun givenALocalRoomSummary(
            aCreateRoomParams: CreateRoomParams,
            aCreationState: LocalRoomCreationState = LocalRoomCreationState.NOT_CREATED,
            aReplacementRoomId: String? = null
    ): LocalRoomSummary {
        val aLocalRoomSummary = LocalRoomSummary(
                roomId = A_LOCAL_ROOM_ID,
                roomSummary = mockk(relaxed = true),
                createRoomParams = aCreateRoomParams,
                creationState = aCreationState,
                replacementRoomId = aReplacementRoomId,
        )
        fakeRoomSummaryDataSource.givenGetLocalRoomSummaryReturns(A_LOCAL_ROOM_ID, aLocalRoomSummary)
        return aLocalRoomSummary
    }

    private fun givenALocalRoomSummaryEntity(
            aCreateRoomParams: CreateRoomParams,
            aCreationState: LocalRoomCreationState = LocalRoomCreationState.NOT_CREATED,
            aReplacementRoomId: String? = null
    ): LocalRoomSummaryEntity {
        val aLocalRoomSummaryEntity = spyk(LocalRoomSummaryEntity(
                roomId = A_LOCAL_ROOM_ID,
                roomSummaryEntity = mockk(relaxed = true),
                replacementRoomId = aReplacementRoomId,
        ).apply {
            createRoomParams = aCreateRoomParams
            creationState = aCreationState
        })
        every {
            fakeMonarchy.fakeRealm.instance
                    .where<LocalRoomSummaryEntity>()
                    .equalTo(LocalRoomSummaryEntityFields.ROOM_ID, A_LOCAL_ROOM_ID)
                    .findFirst()
        } returns aLocalRoomSummaryEntity
        return aLocalRoomSummaryEntity
    }
}
