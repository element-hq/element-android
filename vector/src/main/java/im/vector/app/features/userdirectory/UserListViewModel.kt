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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.toggle
import im.vector.app.core.platform.VectorViewModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.profile.ProfileService
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.rx.rx
import java.util.concurrent.TimeUnit

private typealias KnownUsersSearch = String
private typealias DirectoryUsersSearch = String

class UserListViewModel @AssistedInject constructor(@Assisted initialState: UserListViewState,
                                                    private val session: Session)
    : VectorViewModel<UserListViewState, UserListAction, UserListViewEvents>(initialState) {

    private val knownUsersSearch = BehaviorRelay.create<KnownUsersSearch>()
    private val directoryUsersSearch = BehaviorRelay.create<DirectoryUsersSearch>()

    @AssistedFactory
    interface Factory {
        fun create(initialState: UserListViewState): UserListViewModel
    }

    companion object : MvRxViewModelFactory<UserListViewModel, UserListViewState> {

        override fun create(viewModelContext: ViewModelContext, state: UserListViewState): UserListViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    init {
        observeUsers()
    }

    override fun handle(action: UserListAction) {
        when (action) {
            is UserListAction.SearchUsers -> handleSearchUsers(action.value)
            is UserListAction.ClearSearchUsers -> handleClearSearchUsers()
            is UserListAction.AddPendingSelection -> handleSelectUser(action)
            is UserListAction.RemovePendingSelection -> handleRemoveSelectedUser(action)
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
            _viewEvents.post(UserListViewEvents.OpenShareMatrixToLink(it))
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

        directoryUsersSearch
                .debounce(300, TimeUnit.MILLISECONDS)
                .switchMapSingle { search ->
                    val stream = if (search.isBlank()) {
                        Single.just(emptyList<User>())
                    } else {
                        val searchObservable = session.rx()
                                .searchUsersDirectory(search, 50, state.excludedUserIds.orEmpty())
                                .map { users ->
                                    users.sortedBy { it.toMatrixItem().firstLetterOfDisplayName() }
                                }
                        // If it's a valid user id try to use Profile API
                        // because directory only returns users that are in public rooms or share a room with you, where as
                        // profile will work other federations
                        if (!MatrixPatterns.isUserId(search)) {
                            searchObservable
                        } else {
                            val profileObservable = session.rx().getProfileInfo(search)
                                    .map { json ->
                                        User(
                                                userId = search,
                                                displayName = json[ProfileService.DISPLAY_NAME_KEY] as? String,
                                                avatarUrl = json[ProfileService.AVATAR_URL_KEY] as? String
                                        ).toOptional()
                                    }
                                    .onErrorReturn {
                                        // Profile API can be restricted and doesn't have to return result.
                                        // In this case allow inviting valid user ids.
                                        User(
                                                userId = search,
                                                displayName = null,
                                                avatarUrl = null
                                        ).toOptional()
                                    }

                            Single.zip(
                                    searchObservable,
                                    profileObservable,
                                    { searchResults, optionalProfile ->
                                        val profile = optionalProfile.getOrNull() ?: return@zip searchResults
                                        val searchContainsProfile = searchResults.any { it.userId == profile.userId }
                                        if (searchContainsProfile) {
                                            searchResults
                                        } else {
                                            listOf(profile) + searchResults
                                        }
                                    }
                            )
                        }
                    }
                    stream.toAsync {
                        copy(directoryUsers = it)
                    }
                }
                .subscribe()
                .disposeOnClear()
    }

    private fun handleSelectUser(action: UserListAction.AddPendingSelection) = withState { state ->
        val selections = state.pendingSelections.toggle(action.pendingSelection, singleElement = state.singleSelection)
        setState { copy(pendingSelections = selections) }
    }

    private fun handleRemoveSelectedUser(action: UserListAction.RemovePendingSelection) = withState { state ->
        val selections = state.pendingSelections.minus(action.pendingSelection)
        setState { copy(pendingSelections = selections) }
    }
}
