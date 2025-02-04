/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features

import com.airbnb.mvrx.Success
import im.vector.app.core.epoxy.profiles.ProfileMatrixItemWithPowerLevelWithPresence
import im.vector.app.features.roomprofile.members.RoomMemberListCategories
import im.vector.app.features.roomprofile.members.RoomMemberListController
import im.vector.app.features.roomprofile.members.RoomMemberListViewState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Ignore
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.UserVerificationLevel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RoomMemberListControllerTest {

    @Test
    @Ignore("Too flaky")
    fun testControllerUserVerificationLevel() = runTest {
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

        suspendCoroutine { continuation ->
            roomListController.setData(state)
            roomListController.addModelBuildListener {
                continuation.resume(it)
            }
        }

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
