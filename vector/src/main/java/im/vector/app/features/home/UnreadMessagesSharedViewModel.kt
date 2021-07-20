/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.AppStateHandler
import im.vector.app.RoomGroupingMethod
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.invite.AutoAcceptInvites
import im.vector.app.features.settings.VectorPreferences
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.matrix.android.sdk.api.query.ActiveSpaceFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import org.matrix.android.sdk.rx.asObservable
import java.util.concurrent.TimeUnit

data class UnreadMessagesState(
        val homeSpaceUnread: RoomAggregateNotificationCount = RoomAggregateNotificationCount(0, 0),
        val otherSpacesUnread: RoomAggregateNotificationCount = RoomAggregateNotificationCount(0, 0)
) : MvRxState

data class CountInfo(
        val homeCount: RoomAggregateNotificationCount,
        val otherCount: RoomAggregateNotificationCount
)

class UnreadMessagesSharedViewModel @AssistedInject constructor(@Assisted initialState: UnreadMessagesState,
                                                                session: Session,
                                                                private val vectorPreferences: VectorPreferences,
                                                                appStateHandler: AppStateHandler,
                                                                private val autoAcceptInvites: AutoAcceptInvites)
    : VectorViewModel<UnreadMessagesState, EmptyAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: UnreadMessagesState): UnreadMessagesSharedViewModel
    }

    companion object : MvRxViewModelFactory<UnreadMessagesSharedViewModel, UnreadMessagesState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: UnreadMessagesState): UnreadMessagesSharedViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    override fun handle(action: EmptyAction) {}

    init {

        session.getPagedRoomSummariesLive(
                roomSummaryQueryParams {
                    this.memberships = listOf(Membership.JOIN)
                    this.activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(null)
                }, sortOrder = RoomSortOrder.NONE
        ).asObservable()
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .execute {
                    val counts = session.getNotificationCountForRooms(
                            roomSummaryQueryParams {
                                this.memberships = listOf(Membership.JOIN)
                                this.activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(null)
                            }
                    )
                    val invites = if (autoAcceptInvites.hideInvites) {
                        0
                    } else {
                        session.getRoomSummaries(
                                roomSummaryQueryParams {
                                    this.memberships = listOf(Membership.INVITE)
                                    this.activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(null)
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

        Observable.combineLatest(
                appStateHandler.selectedRoomGroupingObservable.distinctUntilChanged(),
                appStateHandler.selectedRoomGroupingObservable.switchMap {
                    session.getPagedRoomSummariesLive(
                            roomSummaryQueryParams {
                                this.memberships = Membership.activeMemberships()
                            }, sortOrder = RoomSortOrder.NONE
                    ).asObservable()
                            .throttleFirst(300, TimeUnit.MILLISECONDS)
                            .observeOn(Schedulers.computation())
                },
                { groupingMethod, _ ->
                    when (groupingMethod.orNull()) {
                        is RoomGroupingMethod.ByLegacyGroup -> {
                            // currently not supported
                            CountInfo(
                                    RoomAggregateNotificationCount(0, 0),
                                    RoomAggregateNotificationCount(0, 0)
                            )
                        }
                        is RoomGroupingMethod.BySpace       -> {
                            val selectedSpace = appStateHandler.safeActiveSpaceId()

                            val inviteCount = if (autoAcceptInvites.hideInvites) {
                                0
                            } else {
                                session.getRoomSummaries(
                                        roomSummaryQueryParams { this.memberships = listOf(Membership.INVITE) }
                                ).size
                            }
                            val totalCount = session.getNotificationCountForRooms(
                                    roomSummaryQueryParams {
                                        this.memberships = listOf(Membership.JOIN)
                                        this.activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(null).takeIf {
                                            vectorPreferences.labsSpacesOnlyOrphansInHome()
                                        } ?: ActiveSpaceFilter.None
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
                                            rootCounts.fold(0, { acc, rs ->
                                                acc + rs.notificationCount
                                            }) + (counts.notificationCount.takeIf { selectedSpace != null } ?: 0),
                                            rootCounts.fold(0, { acc, rs ->
                                                acc + rs.highlightCount
                                            }) + (counts.highlightCount.takeIf { selectedSpace != null } ?: 0)
                                    )
                            )
                        }
                        null                                -> {
                            CountInfo(
                                    RoomAggregateNotificationCount(0, 0),
                                    RoomAggregateNotificationCount(0, 0)
                            )
                        }
                    }
                }
        ).execute {
            copy(
                    homeSpaceUnread = it.invoke()?.homeCount ?: RoomAggregateNotificationCount(0, 0),
                    otherSpacesUnread = it.invoke()?.otherCount ?: RoomAggregateNotificationCount(0, 0)
            )
        }
    }
}
