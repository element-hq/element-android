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

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.jakewharton.rxrelay2.BehaviorRelay
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.toggle
import im.vector.app.core.platform.VectorViewModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.profile.ProfileService
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.rx.rx
import java.util.concurrent.TimeUnit

private typealias KnownUsersSearch = String
private typealias DirectoryUsersSearch = String

class UserListViewModel @AssistedInject constructor(@Assisted initialState: UserListViewState,
                                                    @Assisted args: UserListFragmentArgs,
                                                    private val session: Session)
    : VectorViewModel<UserListViewState, UserListAction, UserListViewEvents>(initialState) {

    private val knownUsersSearch = BehaviorRelay.create<KnownUsersSearch>()
    private val directoryUsersSearch = BehaviorRelay.create<DirectoryUsersSearch>()

    private var currentUserSearchDisposable: Disposable? = null

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: UserListViewState, args: UserListFragmentArgs): UserListViewModel
    }

    companion object : MvRxViewModelFactory<UserListViewModel, UserListViewState> {

        private val USER_NOT_FOUND_MAP = emptyMap<String, Any>()
        private val USER_NOT_FOUND = User("")

        override fun create(viewModelContext: ViewModelContext, state: UserListViewState): UserListViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            val args = viewModelContext.args<UserListFragmentArgs>()
            return factory?.create(state, args) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    init {
        setState {
            copy(
                    myUserId = session.myUserId,
                    existingRoomId = args.existingRoomId
            )
        }
        observeUsers()
    }

    override fun handle(action: UserListAction) {
        when (action) {
            is UserListAction.SearchUsers                -> handleSearchUsers(action.value)
            is UserListAction.ClearSearchUsers           -> handleClearSearchUsers()
            is UserListAction.SelectPendingInvitee       -> handleSelectUser(action)
            is UserListAction.RemovePendingInvitee       -> handleRemoveSelectedUser(action)
            UserListAction.ComputeMatrixToLinkForSharing -> handleShareMyMatrixToLink()
        }.exhaustive
    }

    private fun handleSearchUsers(searchTerm: String) {
        setState {
            copy(searchTerm = searchTerm)
        }
        knownUsersSearch.accept(searchTerm)
        directoryUsersSearch.accept(searchTerm)
    }

    private fun handleShareMyMatrixToLink() {
        session.permalinkService().createPermalink(session.myUserId)?.let {
            _viewEvents.post(UserListViewEvents.OpenShareMatrixToLing(it))
        }
    }

    private fun handleClearSearchUsers() {
        knownUsersSearch.accept("")
        directoryUsersSearch.accept("")
        setState {
            copy(searchTerm = "")
        }
    }

    private fun observeUsers() = withState { state ->
        knownUsersSearch
                .throttleLast(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .switchMap {
                    session.rx().livePagedUsers(it, state.excludedUserIds)
                }
                .execute { async ->
                    copy(knownUsers = async)
                }

        currentUserSearchDisposable?.dispose()
        directoryUsersSearch
                .debounce(300, TimeUnit.MILLISECONDS)
                .switchMapSingle { search ->
                    val stream = if (search.isBlank()) {
                        Single.just(emptyList())
                    } else if (MatrixPatterns.isUserId(search)) {
                        // If it's a valid user id try to use Profile API
                        // because directory only returns users that are in public rooms or share a room with you, where as
                        // profile will work other federations
                        session.rx().searchUsersDirectory(search, 50, state.excludedUserIds ?: emptySet())
                                .map { users ->
                                    users.sortedBy { it.toMatrixItem().firstLetterOfDisplayName() }
                                }
                                .zipWith(
                                        session.rx().getProfileInfo(search)
                                                // ... not sure how to handle that properly (manage error case in map and return optional)
                                                .onErrorReturn { USER_NOT_FOUND_MAP }
                                                .map { json ->
                                                    if (json === USER_NOT_FOUND_MAP) {
                                                        USER_NOT_FOUND
                                                    } else {
                                                        User(
                                                                userId = search,
                                                                displayName = json[ProfileService.DISPLAY_NAME_KEY] as? String,
                                                                avatarUrl = json[ProfileService.AVATAR_URL_KEY] as? String
                                                        )
                                                    }
                                                },
                                        { t1, t2 ->
                                            if (t2 == USER_NOT_FOUND) {
                                                t1
                                            }
                                            // profile result might also be in search results, in this case keep search result
                                            else if (t1.indexOfFirst { it.userId == t2.userId } != -1) {
                                                t1
                                            } else {
                                                // put it first
                                                listOf(t2) + t1
                                            }
                                        }
                                )
                                .doOnSubscribe {
                                    currentUserSearchDisposable = it
                                }
                                .doOnDispose {
                                    currentUserSearchDisposable = null
                                }
                    } else {
                        session.rx()
                                .searchUsersDirectory(search, 50, state.excludedUserIds ?: emptySet())
                                .map { users ->
                                    users.sortedBy { it.toMatrixItem().firstLetterOfDisplayName() }
                                }
                                .doOnSubscribe {
                                    currentUserSearchDisposable = it
                                }
                                .doOnDispose {
                                    currentUserSearchDisposable = null
                                }
                    }
                    stream.toAsync {
                        copy(directoryUsers = it)
                    }
                }
                .subscribe()
                .disposeOnClear()
    }

    private fun handleSelectUser(action: UserListAction.SelectPendingInvitee) = withState { state ->
        val selectedUsers = state.pendingInvitees.toggle(action.pendingInvitee)
        setState { copy(pendingInvitees = selectedUsers) }
    }

    private fun handleRemoveSelectedUser(action: UserListAction.RemovePendingInvitee) = withState { state ->
        val selectedUsers = state.pendingInvitees.minus(action.pendingInvitee)
        setState { copy(pendingInvitees = selectedUsers) }
    }
}
