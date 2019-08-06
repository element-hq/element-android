/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotx.features.home.createdirect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.core.Option
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.jakewharton.rxrelay2.BehaviorRelay
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.api.session.user.model.User
import im.vector.matrix.android.internal.util.firstLetterOfDisplayName
import im.vector.matrix.rx.rx
import im.vector.riotx.core.extensions.postLiveEvent
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.utils.LiveEvent
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

private typealias KnowUsersFilter = String
private typealias DirectoryUsersSearch = String

data class SelectUserAction(
        val user: User,
        val isAdded: Boolean,
        val index: Int
)

class CreateDirectRoomViewModel @AssistedInject constructor(@Assisted
                                                            initialState: CreateDirectRoomViewState,
                                                            private val session: Session)
    : VectorViewModel<CreateDirectRoomViewState>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: CreateDirectRoomViewState): CreateDirectRoomViewModel
    }

    private val knownUsersFilter = BehaviorRelay.createDefault<Option<KnowUsersFilter>>(Option.empty())
    private val directoryUsersSearch = BehaviorRelay.create<DirectoryUsersSearch>()

    private val _selectUserEvent = MutableLiveData<LiveEvent<SelectUserAction>>()
    val selectUserEvent: LiveData<LiveEvent<SelectUserAction>>
        get() = _selectUserEvent

    companion object : MvRxViewModelFactory<CreateDirectRoomViewModel, CreateDirectRoomViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: CreateDirectRoomViewState): CreateDirectRoomViewModel? {
            val activity: CreateDirectRoomActivity = (viewModelContext as ActivityViewModelContext).activity()
            return activity.createDirectRoomViewModelFactory.create(state)
        }
    }

    init {
        observeKnownUsers()
        observeDirectoryUsers()
    }

    fun handle(action: CreateDirectRoomActions) {
        when (action) {
            is CreateDirectRoomActions.CreateRoomAndInviteSelectedUsers -> createRoomAndInviteSelectedUsers()
            is CreateDirectRoomActions.FilterKnownUsers                 -> knownUsersFilter.accept(Option.just(action.value))
            is CreateDirectRoomActions.ClearFilterKnownUsers            -> knownUsersFilter.accept(Option.empty())
            is CreateDirectRoomActions.SearchDirectoryUsers             -> directoryUsersSearch.accept(action.value)
            is CreateDirectRoomActions.SelectUser                       -> handleSelectUser(action)
            is CreateDirectRoomActions.RemoveSelectedUser               -> handleRemoveSelectedUser(action)
        }
    }

    private fun createRoomAndInviteSelectedUsers() = withState { currentState ->
        val isDirect = currentState.selectedUsers.size == 1
        val roomParams = CreateRoomParams().apply {
            invitedUserIds = ArrayList(currentState.selectedUsers.map { it.userId })
            if (isDirect) {
                setDirectMessage()
            }
        }
        session.rx()
                .createRoom(roomParams)
                .execute {
                    copy(createAndInviteState = it)
                }
    }

    private fun handleRemoveSelectedUser(action: CreateDirectRoomActions.RemoveSelectedUser) = withState { state ->
        val index = state.selectedUsers.indexOfFirst { it.userId == action.user.userId }
        val selectedUsers = state.selectedUsers.minus(action.user)
        setState { copy(selectedUsers = selectedUsers) }
        _selectUserEvent.postLiveEvent(SelectUserAction(action.user, false, index))
    }

    private fun handleSelectUser(action: CreateDirectRoomActions.SelectUser) = withState { state ->
        //Reset the filter asap
        directoryUsersSearch.accept("")
        val isAddOperation: Boolean
        val selectedUsers: Set<User>
        val indexOfUser = state.selectedUsers.indexOfFirst { it.userId == action.user.userId }
        val changeIndex: Int
        if (indexOfUser == -1) {
            changeIndex = state.selectedUsers.size
            selectedUsers = state.selectedUsers.plus(action.user)
            isAddOperation = true
        } else {
            changeIndex = indexOfUser
            selectedUsers = state.selectedUsers.minus(action.user)
            isAddOperation = false
        }
        setState { copy(selectedUsers = selectedUsers) }
        _selectUserEvent.postLiveEvent(SelectUserAction(action.user, isAddOperation, changeIndex))
    }

    private fun observeDirectoryUsers() {
        directoryUsersSearch
                .debounce(300, TimeUnit.MILLISECONDS)
                .switchMapSingle { search ->
                    val stream = if (search.isBlank()) {
                        Single.just(emptyList())
                    } else {
                        session.rx()
                                .searchUsersDirectory(search, 50, emptySet())
                                .map { users ->
                                    users.sortedBy { it.displayName.firstLetterOfDisplayName() }
                                }
                    }
                    stream.toAsync {
                        copy(directoryUsers = it, directorySearchTerm = search)
                    }
                }
                .subscribe()
                .disposeOnClear()
    }

    private fun observeKnownUsers() {
        knownUsersFilter
                .throttleLast(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .switchMap {
                    session.rx().livePagedUsers(it.orNull())
                }
                .execute { async ->
                    copy(
                            knownUsers = async,
                            filterKnownUsersValue = knownUsersFilter.value ?: Option.empty()
                    )
                }
    }

}