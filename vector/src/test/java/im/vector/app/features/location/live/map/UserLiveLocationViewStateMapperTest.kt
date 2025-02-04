/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live.map

import android.graphics.drawable.Drawable
import im.vector.app.features.location.LocationData
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeLocationPinProvider
import im.vector.app.test.fakes.FakeSession
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.getUser
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import org.matrix.android.sdk.api.session.room.model.message.LocationInfo
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem

private const val A_USER_ID = "@aUserId:matrix.org"
private const val A_USER_DISPLAY_NAME = "A_USER_DISPLAY_NAME"
private const val A_IS_ACTIVE = true
private const val A_END_OF_LIVE_TIMESTAMP = 123L
private const val A_LOCATION_TIMESTAMP = 122L
private const val A_LATITUDE = 40.05
private const val A_LONGITUDE = 29.24
private const val A_UNCERTAINTY = 30.0
private const val A_GEO_URI = "geo:$A_LATITUDE,$A_LONGITUDE;u=$A_UNCERTAINTY"

class UserLiveLocationViewStateMapperTest {

    private val locationPinProvider = FakeLocationPinProvider()
    private val fakeSession = FakeSession()
    private val fakeActiveSessionHolder = FakeActiveSessionHolder(fakeSession)

    private val userLiveLocationViewStateMapper = UserLiveLocationViewStateMapper(locationPinProvider.instance, fakeActiveSessionHolder.instance)

    @Before
    fun setup() {
        mockkStatic("org.matrix.android.sdk.api.util.MatrixItemKt")
        val fakeUser = mockk<User>()
        every { fakeSession.getUser(A_USER_ID) } returns fakeUser
        every { fakeUser.toMatrixItem() } returns MatrixItem.UserItem(id = A_USER_ID, displayName = A_USER_DISPLAY_NAME, avatarUrl = "")
    }

    @After
    fun tearDown() {
        unmockkStatic("org.matrix.android.sdk.api.util.MatrixItemKt")
    }

    @Test
    fun `given a summary with invalid data then result is null`() = runTest {
        val summary1 = LiveLocationShareAggregatedSummary(
                roomId = null,
                userId = null,
                isActive = true,
                endOfLiveTimestampMillis = null,
                lastLocationDataContent = null,
        )
        val summary2 = summary1.copy(userId = "")
        val summaryWithoutLocation = summary1.copy(userId = A_USER_ID)

        val viewState1 = userLiveLocationViewStateMapper.map(summary1)
        val viewState2 = userLiveLocationViewStateMapper.map(summary2)
        val viewState3 = userLiveLocationViewStateMapper.map(summaryWithoutLocation)

        viewState1 shouldBeEqualTo null
        viewState2 shouldBeEqualTo null
        viewState3 shouldBeEqualTo null
    }

    @Test
    fun `given a summary with valid data then result is correctly mapped`() = runTest {
        val pinDrawable = mockk<Drawable>()

        val locationDataContent = MessageBeaconLocationDataContent(
                locationInfo = LocationInfo(geoUri = A_GEO_URI),
                unstableTimestampMillis = A_LOCATION_TIMESTAMP
        )
        val summary = LiveLocationShareAggregatedSummary(
                roomId = null,
                userId = A_USER_ID,
                isActive = A_IS_ACTIVE,
                endOfLiveTimestampMillis = A_END_OF_LIVE_TIMESTAMP,
                lastLocationDataContent = locationDataContent,
        )
        val matrixItem = MatrixItem.UserItem(id = A_USER_ID, displayName = A_USER_DISPLAY_NAME, avatarUrl = "")
        locationPinProvider.givenCreateForMatrixItem(matrixItem, pinDrawable)

        val viewState = userLiveLocationViewStateMapper.map(summary)

        val expectedViewState = UserLiveLocationViewState(
                matrixItem = matrixItem,
                pinDrawable = pinDrawable,
                locationData = LocationData(
                        latitude = A_LATITUDE,
                        longitude = A_LONGITUDE,
                        uncertainty = A_UNCERTAINTY
                ),
                endOfLiveTimestampMillis = A_END_OF_LIVE_TIMESTAMP,
                locationTimestampMillis = A_LOCATION_TIMESTAMP,
                showStopSharingButton = false
        )
        viewState shouldBeEqualTo expectedViewState
    }
}
