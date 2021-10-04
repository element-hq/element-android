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

package im.vector.app.features.invite

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import im.vector.app.R
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.userdirectory.PendingSelection
import io.reactivex.Observable
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.rx.rx

class InviteUsersToRoomViewModel @AssistedInject constructor(@Assisted
                                                             initialState: InviteUsersToRoomViewState,
                                                             session: Session,
                                                             val stringProvider: StringProvider)
    : VectorViewModel<InviteUsersToRoomViewState, InviteUsersToRoomAction, InviteUsersToRoomViewEvents>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!

    @AssistedFactory
    interface Factory {
        fun create(initialState: InviteUsersToRoomViewState): InviteUsersToRoomViewModel
    }

    companion object : MvRxViewModelFactory<InviteUsersToRoomViewModel, InviteUsersToRoomViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: InviteUsersToRoomViewState): InviteUsersToRoomViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    override fun handle(action: InviteUsersToRoomAction) {
        when (action) {
            is InviteUsersToRoomAction.InviteSelectedUsers -> inviteUsersToRoom(action.selections)
        }
    }

    private fun inviteUsersToRoom(selections: Set<PendingSelection>) {
        _viewEvents.post(InviteUsersToRoomViewEvents.Loading)

        Observable.fromIterable(selections).flatMapCompletable { user ->
            when (user) {
                is PendingSelection.UserPendingSelection     -> room.rx().invite(user.user.userId, null)
                is PendingSelection.ThreePidPendingSelection -> room.rx().invite3pid(user.threePid)
            }
        }.subscribe(
                {
                    val successMessage = when (selections.size) {
                        1    -> stringProvider.getString(R.string.invitation_sent_to_one_user,
                                selections.first().getBestName())
                        2    -> stringProvider.getString(R.string.invitations_sent_to_two_users,
                                selections.first().getBestName(),
                                selections.last().getBestName())
                        else -> stringProvider.getQuantityString(R.plurals.invitations_sent_to_one_and_more_users,
                                selections.size - 1,
                                selections.first().getBestName(),
                                selections.size - 1)
                    }
                    _viewEvents.post(InviteUsersToRoomViewEvents.Success(successMessage))
                },
                {
                    _viewEvents.post(InviteUsersToRoomViewEvents.Failure(it))
                })
                .disposeOnClear()
    }

    fun getUserIdsOfRoomMembers(): Set<String> {
        return room.roomSummary()?.otherMemberIds?.toSet().orEmpty()
    }
}
