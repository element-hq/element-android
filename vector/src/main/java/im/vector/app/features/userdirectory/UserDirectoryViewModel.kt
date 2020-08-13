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

package im.vector.app.features.userdirectory

import androidx.fragment.app.FragmentActivity
import arrow.core.Option
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.jakewharton.rxrelay2.BehaviorRelay
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.matrix.android.sdk.api.session.Session
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.toggle
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.createdirect.CreateDirectRoomActivity
import im.vector.app.features.invite.InviteUsersToRoomActivity
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.rx.rx
import java.util.concurrent.TimeUnit

private typealias KnowUsersFilter = String
private typealias DirectoryUsersSearch = String

class UserDirectoryViewModel @AssistedInject constructor(@Assisted
                                                         initialState: UserDirectoryViewState,
                                                         private val session: Session)
    : VectorViewModel<UserDirectoryViewState, UserDirectoryAction, UserDirectoryViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: UserDirectoryViewState): UserDirectoryViewModel
    }

    private val knownUsersFilter = BehaviorRelay.createDefault<Option<KnowUsersFilter>>(Option.empty())
    private val directoryUsersSearch = BehaviorRelay.create<DirectoryUsersSearch>()

    companion object : MvRxViewModelFactory<UserDirectoryViewModel, UserDirectoryViewState> {

        override fun create(viewModelContext: ViewModelContext, state: UserDirectoryViewState): UserDirectoryViewModel? {
            return when (viewModelContext) {
                is FragmentViewModelContext -> (viewModelContext.fragment() as KnownUsersFragment).userDirectoryViewModelFactory.create(state)
                is ActivityViewModelContext -> {
                    when (viewModelContext.activity<FragmentActivity>()) {
                        is CreateDirectRoomActivity  -> viewModelContext.activity<CreateDirectRoomActivity>().userDirectoryViewModelFactory.create(state)
                        is InviteUsersToRoomActivity -> viewModelContext.activity<InviteUsersToRoomActivity>().userDirectoryViewModelFactory.create(state)
                        else                         -> error("Wrong activity or fragment")
                    }
                }
                else                        -> error("Wrong activity or fragment")
            }
        }
    }

    init {
        observeKnownUsers()
        observeDirectoryUsers()
    }

    override fun handle(action: UserDirectoryAction) {
        when (action) {
            is UserDirectoryAction.FilterKnownUsers      -> knownUsersFilter.accept(Option.just(action.value))
            is UserDirectoryAction.ClearFilterKnownUsers -> knownUsersFilter.accept(Option.empty())
            is UserDirectoryAction.SearchDirectoryUsers  -> directoryUsersSearch.accept(action.value)
            is UserDirectoryAction.SelectPendingInvitee  -> handleSelectUser(action)
            is UserDirectoryAction.RemovePendingInvitee  -> handleRemoveSelectedUser(action)
        }.exhaustive
    }

    private fun handleRemoveSelectedUser(action: UserDirectoryAction.RemovePendingInvitee) = withState { state ->
        val selectedUsers = state.pendingInvitees.minus(action.pendingInvitee)
        setState { copy(pendingInvitees = selectedUsers) }
    }

    private fun handleSelectUser(action: UserDirectoryAction.SelectPendingInvitee) = withState { state ->
        // Reset the filter asap
        directoryUsersSearch.accept("")
        val selectedUsers = state.pendingInvitees.toggle(action.pendingInvitee)
        setState { copy(pendingInvitees = selectedUsers) }
    }

    private fun observeDirectoryUsers() = withState { state ->
        directoryUsersSearch
                .debounce(300, TimeUnit.MILLISECONDS)
                .switchMapSingle { search ->
                    val stream = if (search.isBlank()) {
                        Single.just(emptyList())
                    } else {
                        session.rx()
                                .searchUsersDirectory(search, 50, state.excludedUserIds ?: emptySet())
                                .map { users ->
                                    users.sortedBy { it.toMatrixItem().firstLetterOfDisplayName() }
                                }
                    }
                    stream.toAsync {
                        copy(directoryUsers = it, directorySearchTerm = search)
                    }
                }
                .subscribe()
                .disposeOnClear()
    }

    private fun observeKnownUsers() = withState { state ->
        knownUsersFilter
                .throttleLast(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .switchMap {
                    session.rx().livePagedUsers(it.orNull(), state.excludedUserIds)
                }
                .execute { async ->
                    copy(
                            knownUsers = async,
                            filterKnownUsersValue = knownUsersFilter.value ?: Option.empty()
                    )
                }
    }
}
