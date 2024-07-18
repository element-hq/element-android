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

package org.matrix.android.sdk.internal.session.room.location

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.room.location.UpdateLiveLocationShareResult
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.database.mapper.LiveLocationShareAggregatedSummaryMapper
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntityFields
import org.matrix.android.sdk.test.fakes.FakeMonarchy
import org.matrix.android.sdk.test.fakes.givenEqualTo
import org.matrix.android.sdk.test.fakes.givenIsNotEmpty
import org.matrix.android.sdk.test.fakes.givenIsNotNull

private const val A_ROOM_ID = "room_id"
private const val AN_EVENT_ID = "event_id"
private const val A_LATITUDE = 1.4
private const val A_LONGITUDE = 40.0
private const val AN_UNCERTAINTY = 5.0
private const val A_TIMEOUT = 15_000L
private const val A_REASON = "reason"

@ExperimentalCoroutinesApi
internal class DefaultLocationSharingServiceTest {

    private val fakeMonarchy = FakeMonarchy()
    private val sendStaticLocationTask = mockk<SendStaticLocationTask>()
    private val sendLiveLocationTask = mockk<SendLiveLocationTask>()
    private val startLiveLocationShareTask = mockk<StartLiveLocationShareTask>()
    private val stopLiveLocationShareTask = mockk<StopLiveLocationShareTask>()
    private val checkIfExistingActiveLiveTask = mockk<CheckIfExistingActiveLiveTask>()
    private val redactLiveLocationShareTask = mockk<RedactLiveLocationShareTask>()
    private val fakeLiveLocationShareAggregatedSummaryMapper = mockk<LiveLocationShareAggregatedSummaryMapper>()

    private val defaultLocationSharingService = DefaultLocationSharingService(
            roomId = A_ROOM_ID,
            monarchy = fakeMonarchy.instance,
            sendStaticLocationTask = sendStaticLocationTask,
            sendLiveLocationTask = sendLiveLocationTask,
            startLiveLocationShareTask = startLiveLocationShareTask,
            stopLiveLocationShareTask = stopLiveLocationShareTask,
            checkIfExistingActiveLiveTask = checkIfExistingActiveLiveTask,
            redactLiveLocationShareTask = redactLiveLocationShareTask,
            liveLocationShareAggregatedSummaryMapper = fakeLiveLocationShareAggregatedSummaryMapper
    )

    @Before
    fun setUp() {
        mockkStatic("androidx.lifecycle.Transformations")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `static location can be sent`() = runTest {
        val isUserLocation = true
        val cancelable = mockk<Cancelable>()
        coEvery { sendStaticLocationTask.execute(any()) } returns cancelable

        val result = defaultLocationSharingService.sendStaticLocation(
                latitude = A_LATITUDE,
                longitude = A_LONGITUDE,
                uncertainty = AN_UNCERTAINTY,
                isUserLocation = isUserLocation
        )

        result shouldBeEqualTo cancelable
        val expectedParams = SendStaticLocationTask.Params(
                roomId = A_ROOM_ID,
                latitude = A_LATITUDE,
                longitude = A_LONGITUDE,
                uncertainty = AN_UNCERTAINTY,
                isUserLocation = isUserLocation,
        )
        coVerify { sendStaticLocationTask.execute(expectedParams) }
    }

    @Test
    fun `live location can be sent`() = runTest {
        val cancelable = mockk<Cancelable>()
        coEvery { sendLiveLocationTask.execute(any()) } returns cancelable

        val result = defaultLocationSharingService.sendLiveLocation(
                beaconInfoEventId = AN_EVENT_ID,
                latitude = A_LATITUDE,
                longitude = A_LONGITUDE,
                uncertainty = AN_UNCERTAINTY
        )

        result shouldBeEqualTo cancelable
        val expectedParams = SendLiveLocationTask.Params(
                roomId = A_ROOM_ID,
                beaconInfoEventId = AN_EVENT_ID,
                latitude = A_LATITUDE,
                longitude = A_LONGITUDE,
                uncertainty = AN_UNCERTAINTY
        )
        coVerify { sendLiveLocationTask.execute(expectedParams) }
    }

    @Test
    fun `given existing active live can be stopped when starting a live then the current live is stopped and the new live is started`() = runTest {
        coEvery { checkIfExistingActiveLiveTask.execute(any()) } returns true
        coEvery { stopLiveLocationShareTask.execute(any()) } returns UpdateLiveLocationShareResult.Success("stopped-event-id")
        coEvery { startLiveLocationShareTask.execute(any()) } returns UpdateLiveLocationShareResult.Success(AN_EVENT_ID)

        val result = defaultLocationSharingService.startLiveLocationShare(A_TIMEOUT)

        result shouldBeEqualTo UpdateLiveLocationShareResult.Success(AN_EVENT_ID)
        val expectedCheckExistingParams = CheckIfExistingActiveLiveTask.Params(
                roomId = A_ROOM_ID
        )
        coVerify { checkIfExistingActiveLiveTask.execute(expectedCheckExistingParams) }
        val expectedStopParams = StopLiveLocationShareTask.Params(
                roomId = A_ROOM_ID
        )
        coVerify { stopLiveLocationShareTask.execute(expectedStopParams) }
        val expectedStartParams = StartLiveLocationShareTask.Params(
                roomId = A_ROOM_ID,
                timeoutMillis = A_TIMEOUT,
        )
        coVerify { startLiveLocationShareTask.execute(expectedStartParams) }
    }

    @Test
    fun `given existing active live cannot be stopped when starting a live then the result is failure`() = runTest {
        coEvery { checkIfExistingActiveLiveTask.execute(any()) } returns true
        val error = Throwable()
        coEvery { stopLiveLocationShareTask.execute(any()) } returns UpdateLiveLocationShareResult.Failure(error)

        val result = defaultLocationSharingService.startLiveLocationShare(A_TIMEOUT)

        result shouldBeEqualTo UpdateLiveLocationShareResult.Failure(error)
        val expectedCheckExistingParams = CheckIfExistingActiveLiveTask.Params(
                roomId = A_ROOM_ID
        )
        coVerify { checkIfExistingActiveLiveTask.execute(expectedCheckExistingParams) }
        val expectedStopParams = StopLiveLocationShareTask.Params(
                roomId = A_ROOM_ID
        )
        coVerify { stopLiveLocationShareTask.execute(expectedStopParams) }
    }

    @Test
    fun `given no existing active live when starting a live then the new live is started`() = runTest {
        coEvery { checkIfExistingActiveLiveTask.execute(any()) } returns false
        coEvery { startLiveLocationShareTask.execute(any()) } returns UpdateLiveLocationShareResult.Success(AN_EVENT_ID)

        val result = defaultLocationSharingService.startLiveLocationShare(A_TIMEOUT)

        result shouldBeEqualTo UpdateLiveLocationShareResult.Success(AN_EVENT_ID)
        val expectedCheckExistingParams = CheckIfExistingActiveLiveTask.Params(
                roomId = A_ROOM_ID
        )
        coVerify { checkIfExistingActiveLiveTask.execute(expectedCheckExistingParams) }
        val expectedStartParams = StartLiveLocationShareTask.Params(
                roomId = A_ROOM_ID,
                timeoutMillis = A_TIMEOUT,
        )
        coVerify { startLiveLocationShareTask.execute(expectedStartParams) }
    }

    @Test
    fun `live location share can be stopped`() = runTest {
        coEvery { stopLiveLocationShareTask.execute(any()) } returns UpdateLiveLocationShareResult.Success(AN_EVENT_ID)

        val result = defaultLocationSharingService.stopLiveLocationShare()

        result shouldBeEqualTo UpdateLiveLocationShareResult.Success(AN_EVENT_ID)
        val expectedParams = StopLiveLocationShareTask.Params(
                roomId = A_ROOM_ID
        )
        coVerify { stopLiveLocationShareTask.execute(expectedParams) }
    }

    @Test
    fun `live location share can be redacted`() = runTest {
        coEvery { redactLiveLocationShareTask.execute(any()) } just runs

        defaultLocationSharingService.redactLiveLocationShare(beaconInfoEventId = AN_EVENT_ID, reason = A_REASON)

        val expectedParams = RedactLiveLocationShareTask.Params(
                roomId = A_ROOM_ID,
                beaconInfoEventId = AN_EVENT_ID,
                reason = A_REASON
        )
        coVerify { redactLiveLocationShareTask.execute(expectedParams) }
    }

    @Test
    fun `livedata of live summaries is correctly computed`() {
        val entity = LiveLocationShareAggregatedSummaryEntity()
        val summary = LiveLocationShareAggregatedSummary(
                roomId = A_ROOM_ID,
                userId = "",
                isActive = true,
                endOfLiveTimestampMillis = 123,
                lastLocationDataContent = null
        )

        fakeMonarchy.givenWhere<LiveLocationShareAggregatedSummaryEntity>()
                .givenEqualTo(LiveLocationShareAggregatedSummaryEntityFields.ROOM_ID, A_ROOM_ID)
                .givenEqualTo(LiveLocationShareAggregatedSummaryEntityFields.IS_ACTIVE, true)
                .givenIsNotEmpty(LiveLocationShareAggregatedSummaryEntityFields.USER_ID)
                .givenIsNotNull(LiveLocationShareAggregatedSummaryEntityFields.LAST_LOCATION_CONTENT)
        fakeMonarchy.givenFindAllMappedWithChangesReturns(
                realmEntities = listOf(entity),
                mappedResult = listOf(summary),
                fakeLiveLocationShareAggregatedSummaryMapper
        )

        val result = defaultLocationSharingService.getRunningLiveLocationShareSummaries().value

        result shouldBeEqualTo listOf(summary)
    }

    @Test
    fun `given an event id when getting livedata on corresponding live summary then it is correctly computed`() {
        val entity = LiveLocationShareAggregatedSummaryEntity()
        val summary = LiveLocationShareAggregatedSummary(
                roomId = A_ROOM_ID,
                userId = "",
                isActive = true,
                endOfLiveTimestampMillis = 123,
                lastLocationDataContent = null
        )
        fakeMonarchy.givenWhere<LiveLocationShareAggregatedSummaryEntity>()
                .givenEqualTo(LiveLocationShareAggregatedSummaryEntityFields.ROOM_ID, A_ROOM_ID)
                .givenEqualTo(LiveLocationShareAggregatedSummaryEntityFields.EVENT_ID, AN_EVENT_ID)
        fakeMonarchy.givenFindAllMappedWithChangesReturns(
                realmEntities = listOf(entity),
                mappedResult = listOf(summary),
                fakeLiveLocationShareAggregatedSummaryMapper
        )

        val result = defaultLocationSharingService.getLiveLocationShareSummary(AN_EVENT_ID).value

        result shouldBeEqualTo summary.toOptional()
    }
}
