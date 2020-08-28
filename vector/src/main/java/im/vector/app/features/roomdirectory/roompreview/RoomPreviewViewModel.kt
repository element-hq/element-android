/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.roomdirectory.roompreview

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.roomdirectory.JoinState
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.rx.rx
import timber.log.Timber

class RoomPreviewViewModel @AssistedInject constructor(@Assisted private val initialState: RoomPreviewViewState,
                                                       private val session: Session)
    : VectorViewModel<RoomPreviewViewState, RoomPreviewAction, EmptyViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: RoomPreviewViewState): RoomPreviewViewModel
    }

    companion object : MvRxViewModelFactory<RoomPreviewViewModel, RoomPreviewViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomPreviewViewState): RoomPreviewViewModel? {
            val fragment: RoomPreviewNoPreviewFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.roomPreviewViewModelFactory.create(state)
        }
    }

    init {
        // Observe joined room (from the sync)
        observeRoomSummary()
        observeMembershipChanges()
    }

    private fun observeRoomSummary() {
        val queryParams = roomSummaryQueryParams {
            roomId = QueryStringValue.Equals(initialState.roomId)
        }
        session
                .rx()
                .liveRoomSummaries(queryParams)
                .subscribe { list ->
                    val isRoomJoined = list.any {
                        it.membership == Membership.JOIN
                    }
                    if (isRoomJoined) {
                        setState { copy(roomJoinState = JoinState.JOINED) }
                    }
                }
                .disposeOnClear()
    }

    private fun observeMembershipChanges() {
        session.rx()
                .liveRoomChangeMembershipState()
                .subscribe {
                    val changeMembership = it[initialState.roomId] ?: ChangeMembershipState.Unknown
                    val joinState = when (changeMembership) {
                        is ChangeMembershipState.Joining       -> JoinState.JOINING
                        is ChangeMembershipState.FailedJoining -> JoinState.JOINING_ERROR
                        // Other cases are handled by room summary
                        else                                   -> null
                    }
                    if (joinState != null) {
                        setState { copy(roomJoinState = joinState) }
                    }
                }
                .disposeOnClear()
    }

    override fun handle(action: RoomPreviewAction) {
        when (action) {
            is RoomPreviewAction.Join -> handleJoinRoom()
        }.exhaustive
    }

    private fun handleJoinRoom() = withState { state ->
        if (state.roomJoinState == JoinState.JOINING) {
            // Request already sent, should not happen
            Timber.w("Try to join an already joining room. Should not happen")
            return@withState
        }
        session.joinRoom(state.roomId, viaServers = state.homeServers, callback = object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                // We do not update the joiningRoomsIds here, because, the room is not joined yet regarding the sync data.
                // Instead, we wait for the room to be joined
            }

            override fun onFailure(failure: Throwable) {
                setState { copy(lastError = failure) }
            }
        })
    }
}
