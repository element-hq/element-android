/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import androidx.lifecycle.asFlow
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.SpaceStateHandler
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.invite.AutoAcceptInvites
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.core.utils.flow.throttleFirst
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import org.matrix.android.sdk.api.query.SpaceFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.spaceSummaryQueryParams
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount

data class UnreadMessagesState(
        val homeSpaceUnread: RoomAggregateNotificationCount = RoomAggregateNotificationCount(0, 0),
        val otherSpacesUnread: RoomAggregateNotificationCount = RoomAggregateNotificationCount(0, 0)
) : MavericksState

data class CountInfo(
        val homeCount: RoomAggregateNotificationCount,
        val otherCount: RoomAggregateNotificationCount
)

class UnreadMessagesSharedViewModel @AssistedInject constructor(
        @Assisted initialState: UnreadMessagesState,
        session: Session,
        private val vectorPreferences: VectorPreferences,
        spaceStateHandler: SpaceStateHandler,
        private val autoAcceptInvites: AutoAcceptInvites
) :
        VectorViewModel<UnreadMessagesState, EmptyAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<UnreadMessagesSharedViewModel, UnreadMessagesState> {
        override fun create(initialState: UnreadMessagesState): UnreadMessagesSharedViewModel
    }

    companion object : MavericksViewModelFactory<UnreadMessagesSharedViewModel, UnreadMessagesState> by hiltMavericksViewModelFactory()

    override fun handle(action: EmptyAction) {}

    private val roomService = session.roomService()

    init {
        roomService.roomSummariesChangesLive(
                roomSummaryQueryParams {
                    this.memberships = listOf(Membership.JOIN)
                    this.spaceFilter = SpaceFilter.OrphanRooms
                }, sortOrder = RoomSortOrder.NONE
        ).asFlow()
                .throttleFirst(300)
                .execute {
                    val counts = roomService.getNotificationCountForRooms(
                            roomSummaryQueryParams {
                                this.memberships = listOf(Membership.JOIN)
                                this.spaceFilter = SpaceFilter.OrphanRooms
                            }
                    )
                    val invites = if (autoAcceptInvites.hideInvites) {
                        0
                    } else {
                        roomService.getRoomSummaries(
                                roomSummaryQueryParams {
                                    this.memberships = listOf(Membership.INVITE)
                                    this.spaceFilter = SpaceFilter.OrphanRooms
                                }
                        ).size
                    }

                    copy(
                            homeSpaceUnread = RoomAggregateNotificationCount(
                                    counts.notificationCount + invites,
                                    highlightCount = counts.highlightCount + invites
                            )
                    )
                }

        combine(
                spaceStateHandler.getSelectedSpaceFlow().distinctUntilChanged(),
                spaceStateHandler.getSelectedSpaceFlow().flatMapLatest {
                    roomService.roomSummariesChangesLive(
                            roomSummaryQueryParams {
                                this.memberships = Membership.activeMemberships()
                            }, sortOrder = RoomSortOrder.NONE
                    ).asFlow()
                            .throttleFirst(300)
                }
        ) { selectedSpaceOption, _ ->
            val selectedSpace = selectedSpaceOption.orNull()?.roomId

            val inviteCount = if (autoAcceptInvites.hideInvites) {
                0
            } else {
                roomService.getRoomSummaries(
                        roomSummaryQueryParams { this.memberships = listOf(Membership.INVITE) }
                ).size
            }

            val spaceInviteCount = if (autoAcceptInvites.hideInvites) {
                0
            } else {
                roomService.getRoomSummaries(
                        spaceSummaryQueryParams {
                            this.memberships = listOf(Membership.INVITE)
                        }
                ).size
            }

            val totalCount = roomService.getNotificationCountForRooms(
                    roomSummaryQueryParams {
                        this.memberships = listOf(Membership.JOIN)
                        this.spaceFilter = when {
                            vectorPreferences.prefSpacesShowAllRoomInHome() -> SpaceFilter.NoFilter
                            else -> SpaceFilter.OrphanRooms
                        }
                    }
            )

            val counts = RoomAggregateNotificationCount(
                    totalCount.notificationCount + inviteCount,
                    totalCount.highlightCount + inviteCount
            )
            val rootCounts = session.spaceService().getRootSpaceSummaries()
                    .filter {
                        // filter out current selection
                        it.roomId != selectedSpace
                    }

            CountInfo(
                    homeCount = counts,
                    otherCount = RoomAggregateNotificationCount(
                            notificationCount = rootCounts.fold(0) { acc, rs -> acc + rs.notificationCount } +
                                    (counts.notificationCount.takeIf { selectedSpace != null } ?: 0) +
                                    spaceInviteCount,
                            highlightCount = rootCounts.fold(0) { acc, rs -> acc + rs.highlightCount } +
                                    (counts.highlightCount.takeIf { selectedSpace != null } ?: 0) +
                                    spaceInviteCount
                    )
            )
        }
                .flowOn(Dispatchers.Default)
                .execute {
                    copy(
                            homeSpaceUnread = it.invoke()?.homeCount ?: RoomAggregateNotificationCount(0, 0),
                            otherSpacesUnread = it.invoke()?.otherCount ?: RoomAggregateNotificationCount(0, 0)
                    )
                }
    }
}
