/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.room

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.jakewharton.rxrelay2.BehaviorRelay
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.rx.rx
import org.matrix.android.sdk.rx.unwrap
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * This ViewModel observe a room summary and notify when the room is left
 */
class RequireActiveMembershipViewModel @AssistedInject constructor(
        @Assisted initialState: RequireActiveMembershipViewState,
        private val stringProvider: StringProvider,
        private val session: Session)
    : VectorViewModel<RequireActiveMembershipViewState, RequireActiveMembershipAction, RequireActiveMembershipViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: RequireActiveMembershipViewState): RequireActiveMembershipViewModel
    }

    companion object : MvRxViewModelFactory<RequireActiveMembershipViewModel, RequireActiveMembershipViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RequireActiveMembershipViewState): RequireActiveMembershipViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    private val roomIdObservable = BehaviorRelay.createDefault(Optional.from(initialState.roomId))

    init {
        observeRoomSummary()
    }

    private fun observeRoomSummary() {
        roomIdObservable
                .unwrap()
                .switchMap { roomId ->
                    val room = session.getRoom(roomId) ?: return@switchMap Observable.just(Optional.empty<RequireActiveMembershipViewEvents.RoomLeft>())
                    room.rx()
                            .liveRoomSummary()
                            .unwrap()
                            .observeOn(Schedulers.computation())
                            .map { mapToLeftViewEvent(room, it) }
                }
                .unwrap()
                .subscribe { event ->
                    _viewEvents.post(event)
                }
                .disposeOnClear()
    }

    private fun mapToLeftViewEvent(room: Room, roomSummary: RoomSummary): Optional<RequireActiveMembershipViewEvents.RoomLeft> {
        if (roomSummary.membership.isActive()) {
            return Optional.empty()
        }
        val senderId = room.getStateEvent(EventType.STATE_ROOM_MEMBER, QueryStringValue.Equals(session.myUserId))?.senderId
        val senderDisplayName = senderId?.takeIf { it != session.myUserId }?.let {
            room.getRoomMember(it)?.displayName ?: it
        }
        val viewEvent = when (roomSummary.membership) {
            Membership.LEAVE -> {
                val message = senderDisplayName?.let {
                    stringProvider.getString(R.string.has_been_kicked, roomSummary.displayName, it)
                }
                RequireActiveMembershipViewEvents.RoomLeft(message)
            }
            Membership.KNOCK -> {
                val message = senderDisplayName?.let {
                    stringProvider.getString(R.string.has_been_kicked, roomSummary.displayName, it)
                }
                RequireActiveMembershipViewEvents.RoomLeft(message)
            }
            Membership.BAN   -> {
                val message = senderDisplayName?.let {
                    stringProvider.getString(R.string.has_been_banned, roomSummary.displayName, it)
                }
                RequireActiveMembershipViewEvents.RoomLeft(message)
            }
            else             -> null
        }
        return Optional.from(viewEvent)
    }

    override fun handle(action: RequireActiveMembershipAction) {
        when (action) {
            is RequireActiveMembershipAction.ChangeRoom -> {
                setState {
                    copy(roomId = action.roomId)
                }
                roomIdObservable.accept(Optional.from(action.roomId))
            }
        }.exhaustive
    }
}
