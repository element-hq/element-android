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

package im.vector.app.features

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.core.epoxy.profiles.ProfileMatrixItemWithPowerLevelWithPresence
import im.vector.app.features.roomprofile.members.RoomMemberListCategories
import im.vector.app.features.roomprofile.members.RoomMemberListController
import im.vector.app.features.roomprofile.members.RoomMemberListViewState
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.api.session.crypto.model.UserVerificationLevel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomMemberListControllerTest {

    @get:Rule
    val mvrxTestRule = MvRxTestRule()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun testControllerUserVerificationLevel() {
        val roomListController = RoomMemberListController(
                avatarRenderer = mockk {
                },
                stringProvider = mockk {
                    every { getString(any()) } answers {
                        this.args[0].toString()
                    }
                },
                colorProvider = mockk {
                    every { getColorFromAttribute(any()) } returns 0x0
                },
                roomMemberSummaryFilter = mockk(relaxed = true) {
                    every { test(any()) } returns true
                }
        )

        val fakeRoomSummary = RoomSummary(
                roomId = "!roomId",
                displayName = "Fake Room",
                topic = "A topic",
                isEncrypted = true,
                encryptionEventTs = 0,
                typingUsers = emptyList(),
        )

        val state = RoomMemberListViewState(
                roomId = "!roomId",
                roomSummary = Success(fakeRoomSummary),
                areAllMembersLoaded = true,
                roomMemberSummaries = Success(
                        listOf(
                                RoomMemberListCategories.USER to listOf(
                                        RoomMemberSummary(
                                                membership = Membership.JOIN,
                                                userId = "@alice:example.com"
                                        ),
                                        RoomMemberSummary(
                                                membership = Membership.JOIN,
                                                userId = "@bob:example.com"
                                        ),
                                        RoomMemberSummary(
                                                membership = Membership.JOIN,
                                                userId = "@carl:example.com"
                                        ),
                                        RoomMemberSummary(
                                                membership = Membership.JOIN,
                                                userId = "@massy:example.com"
                                        )
                                )
                        )
                ),
                trustLevelMap = Success(
                        mapOf(
                                "@alice:example.com" to UserVerificationLevel.UNVERIFIED_BUT_WAS_PREVIOUSLY,
                                "@bob:example.com" to UserVerificationLevel.VERIFIED_ALL_DEVICES_TRUSTED,
                                "@carl:example.com" to UserVerificationLevel.WAS_NEVER_VERIFIED,
                                "@massy:example.com" to UserVerificationLevel.VERIFIED_WITH_DEVICES_UNTRUSTED,
                        )
                )
        )

        roomListController.setData(state)

        val models = roomListController.adapter.copyOfModels

        val profileItems = models.filterIsInstance<ProfileMatrixItemWithPowerLevelWithPresence>()

        profileItems.firstOrNull {
            it.matrixItem.id == "@alice:example.com"
        }!!.userVerificationLevel shouldBeEqualTo UserVerificationLevel.UNVERIFIED_BUT_WAS_PREVIOUSLY

        profileItems.firstOrNull {
            it.matrixItem.id == "@bob:example.com"
        }!!.userVerificationLevel shouldBeEqualTo UserVerificationLevel.VERIFIED_ALL_DEVICES_TRUSTED

        profileItems.firstOrNull {
            it.matrixItem.id == "@carl:example.com"
        }!!.userVerificationLevel shouldBeEqualTo UserVerificationLevel.WAS_NEVER_VERIFIED

        profileItems.firstOrNull {
            it.matrixItem.id == "@massy:example.com"
        }!!.userVerificationLevel shouldBeEqualTo UserVerificationLevel.VERIFIED_WITH_DEVICES_UNTRUSTED
    }
}
