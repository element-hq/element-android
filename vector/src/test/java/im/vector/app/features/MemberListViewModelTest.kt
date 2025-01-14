/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.features.roomprofile.RoomProfileArgs
import im.vector.app.features.roomprofile.members.RoomMemberListViewModel
import im.vector.app.features.roomprofile.members.RoomMemberListViewState
import im.vector.app.features.roomprofile.members.RoomMemberSummaryComparator
import im.vector.app.test.test
import im.vector.app.test.testCoroutineDispatchers
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.crosssigning.CryptoCrossSigningKey
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.UserVerificationLevel
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.RoomService
import org.matrix.android.sdk.api.session.room.crypto.RoomCryptoService
import org.matrix.android.sdk.api.session.room.members.MembershipService
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.state.StateService
import org.matrix.android.sdk.api.session.user.UserService
import org.matrix.android.sdk.api.util.Optional

class MemberListViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val fakeRoomId = "!roomId"
    private val args = RoomProfileArgs(fakeRoomId)

    private val aliceMxid = "@alice:example.com"
    private val bobMxid = "@bob:example.com"
    private val marcMxid = "@marc:example.com"

    private val aliceDevice1 = CryptoDeviceInfo(
            deviceId = "ALICE_1",
            userId = aliceMxid,
            trustLevel = DeviceTrustLevel(true, true)
    )

    private val aliceDevice2 = CryptoDeviceInfo(
            deviceId = "ALICE_2",
            userId = aliceMxid,
            trustLevel = DeviceTrustLevel(false, false)
    )

    private val bobDevice1 = CryptoDeviceInfo(
            deviceId = "BOB_1",
            userId = bobMxid,
            trustLevel = DeviceTrustLevel(true, true)
    )

    private val bobDevice2 = CryptoDeviceInfo(
            deviceId = "BOB_2",
            userId = bobMxid,
            trustLevel = DeviceTrustLevel(true, true)
    )

    private val markDevice = CryptoDeviceInfo(
            deviceId = "MARK_1",
            userId = marcMxid,
            trustLevel = DeviceTrustLevel(false, true)
    )

    private val fakeMembershipservice: MembershipService = mockk {

        val memberList = mutableListOf<RoomMemberSummary>(
                RoomMemberSummary(Membership.JOIN, aliceMxid, displayName = "Alice"),
                RoomMemberSummary(Membership.JOIN, bobMxid, displayName = "Bob"),
                RoomMemberSummary(Membership.JOIN, marcMxid, displayName = "marc")
        )

        every { getRoomMembers(any()) } returns memberList

        every { getRoomMembersLive(any()) } returns MutableLiveData(memberList)

        every { areAllMembersLoadedLive() } returns MutableLiveData(true)

        coEvery { areAllMembersLoaded() } returns true
    }

    private val fakeRoomCryptoService: RoomCryptoService = mockk {
        every { isEncrypted() } returns true
    }
    private val fakeRoom: Room = mockk {

        val fakeStateService: StateService = mockk {
            every { getStateEventLive(any(), any()) } returns MutableLiveData()
            every { getStateEventsLive(any(), any()) } returns MutableLiveData()
            every { getStateEvent(any(), any()) } returns null
        }

        every { stateService() } returns fakeStateService

        every { coroutineDispatchers } returns testCoroutineDispatchers

        every { getRoomSummaryLive() } returns MutableLiveData<Optional<RoomSummary>>(Optional(fakeRoomSummary))

        every { membershipService() } returns fakeMembershipservice

        every { roomCryptoService() } returns fakeRoomCryptoService

        every { roomSummary() } returns fakeRoomSummary
    }

    private val fakeUserService: UserService = mockk {
        every { getIgnoredUsersLive() } returns MutableLiveData()
    }

    val fakeSession: Session = mockk {

        val fakeCrossSigningService: CrossSigningService = mockk {
            coEvery { isUserTrusted(aliceMxid) } returns true
            coEvery { isUserTrusted(bobMxid) } returns true
            coEvery { isUserTrusted(marcMxid) } returns false

            coEvery { getUserCrossSigningKeys(aliceMxid) } returns MXCrossSigningInfo(
                    aliceMxid,
                    crossSigningKeys = listOf(
                            CryptoCrossSigningKey(
                                    aliceMxid,
                                    usages = listOf("master"),
                                    keys = emptyMap(),
                                    trustLevel = DeviceTrustLevel(true, true),
                                    signatures = emptyMap()
                            ),
                            CryptoCrossSigningKey(
                                    aliceMxid,
                                    usages = listOf("self_signing"),
                                    keys = emptyMap(),
                                    trustLevel = DeviceTrustLevel(true, true),
                                    signatures = emptyMap()
                            ),
                            CryptoCrossSigningKey(
                                    aliceMxid,
                                    usages = listOf("user_signing"),
                                    keys = emptyMap(),
                                    trustLevel = DeviceTrustLevel(true, true),
                                    signatures = emptyMap()
                            )
                    ),
                    true
            )
            coEvery { getUserCrossSigningKeys(bobMxid) } returns MXCrossSigningInfo(
                    aliceMxid,
                    crossSigningKeys = listOf(
                            CryptoCrossSigningKey(
                                    bobMxid,
                                    usages = listOf("master"),
                                    keys = emptyMap(),
                                    trustLevel = DeviceTrustLevel(true, true),
                                    signatures = emptyMap()
                            ),
                            CryptoCrossSigningKey(
                                    bobMxid,
                                    usages = listOf("self_signing"),
                                    keys = emptyMap(),
                                    trustLevel = DeviceTrustLevel(true, true),
                                    signatures = emptyMap()
                            ),
                            CryptoCrossSigningKey(
                                    bobMxid,
                                    usages = listOf("user_signing"),
                                    keys = emptyMap(),
                                    trustLevel = DeviceTrustLevel(true, true),
                                    signatures = emptyMap()
                            )
                    ),
                    true
            )
            coEvery { getUserCrossSigningKeys(marcMxid) } returns MXCrossSigningInfo(
                    aliceMxid,
                    crossSigningKeys = listOf(
                            CryptoCrossSigningKey(
                                    marcMxid,
                                    usages = listOf("master"),
                                    keys = emptyMap(),
                                    trustLevel = DeviceTrustLevel(false, false),
                                    signatures = emptyMap()
                            ),
                            CryptoCrossSigningKey(
                                    marcMxid,
                                    usages = listOf("self_signing"),
                                    keys = emptyMap(),
                                    trustLevel = DeviceTrustLevel(false, false),
                                    signatures = emptyMap()
                            ),
                            CryptoCrossSigningKey(
                                    marcMxid,
                                    usages = listOf("user_signing"),
                                    keys = emptyMap(),
                                    trustLevel = DeviceTrustLevel(false, false),
                                    signatures = emptyMap()
                            )
                    ),
                    true
            )
        }

        val fakeCryptoService: CryptoService = mockk {
            every { crossSigningService() } returns fakeCrossSigningService

            every {
                getLiveCryptoDeviceInfo(listOf(aliceMxid, bobMxid, marcMxid))
            } returns MutableLiveData(
                    listOf(
                            aliceDevice1, aliceDevice2, bobDevice1, bobDevice2, markDevice
                    )
            )
        }

        val fakeRoomService: RoomService = mockk {
            every { getRoom(any()) } returns fakeRoom
        }
        every { roomService() } returns fakeRoomService
        every { userService() } returns fakeUserService
        every { cryptoService() } returns fakeCryptoService
    }

    private val fakeRoomSummary = RoomSummary(
            roomId = fakeRoomId,
            displayName = "Fake Room",
            topic = "A topic",
            isEncrypted = true,
            encryptionEventTs = 0,
            typingUsers = emptyList(),
    )

    @Test
    fun testBasicUserVerificationLevels() {
        val viewModel = createViewModel()
        viewModel
                .test()
                .assertLatestState {
                    val trustMap = it.trustLevelMap.invoke() ?: return@assertLatestState false
                    trustMap[aliceMxid] == UserVerificationLevel.VERIFIED_WITH_DEVICES_UNTRUSTED &&
                            trustMap[bobMxid] == UserVerificationLevel.VERIFIED_ALL_DEVICES_TRUSTED &&
                            trustMap[marcMxid] == UserVerificationLevel.UNVERIFIED_BUT_WAS_PREVIOUSLY
                }
                .finish()
    }

    private fun createViewModel(): RoomMemberListViewModel {
        return RoomMemberListViewModel(
                RoomMemberListViewState(args),
                RoomMemberSummaryComparator(),
                fakeSession,
        )
    }
}
