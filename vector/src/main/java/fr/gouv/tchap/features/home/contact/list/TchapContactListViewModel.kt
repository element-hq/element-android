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

package fr.gouv.tchap.features.home.contact.list

import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import fr.gouv.tchap.core.utils.TchapUtils
import im.vector.app.core.contacts.ContactsDataSource
import im.vector.app.core.contacts.MappedContact
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.toggle
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.displayname.getBestName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.ActiveSpaceFilter
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.profile.ProfileService
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.flow.flow
import timber.log.Timber

class TchapContactListViewModel @AssistedInject constructor(
        @Assisted initialState: TchapContactListViewState,
        private val contactsDataSource: ContactsDataSource,
        private val session: Session
) : VectorViewModel<TchapContactListViewState, TchapContactListAction, TchapContactListViewEvents>(initialState) {

    //    private val knownUsersSearch = BehaviorRelay.create<KnownUsersSearch>()
    private val directoryUsersSearch = MutableStateFlow("")

    // All users from roomSummaries
    private var roomSummariesUsers: List<User> = emptyList()

    // All the contacts on the phone
    private var localUsers: List<User> = emptyList()

    private val isExternalUser = TchapUtils.isExternalTchapUser(session.myUserId)

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<TchapContactListViewModel, TchapContactListViewState> {
        override fun create(initialState: TchapContactListViewState): TchapContactListViewModel
    }

    companion object : MavericksViewModelFactory<TchapContactListViewModel, TchapContactListViewState> by hiltMavericksViewModelFactory() {

        override fun initialState(viewModelContext: ViewModelContext): TchapContactListViewState {
            return TchapContactListViewState(
                    excludedUserIds = null,
                    singleSelection = true,
                    showSearch = true,
                    showInviteActions = false
            )
        }
    }

    init {
        observeUsers()
        observeDMs()

        onEach(TchapContactListViewState::searchTerm) {
            updateFilteredContacts()
        }

        setState {
            copy(
                    identityServerUrl = session.identityService().getCurrentIdentityServerUrl(),
                    userConsent = session.identityService().getUserConsent(),
                    showInviteActions = !isExternalUser,
                    showSearch = !isExternalUser
            )
        }
    }

    private fun handleLoadContacts() {
        setState {
            copy(
                    mappedContacts = Loading()
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val allContacts = contactsDataSource.getContacts(
                    withEmails = true,
                    // Do not handle phone numbers for the moment
                    withMsisdn = false
            )

            setState {
                copy(
                        mappedContacts = Success(allContacts)
                )
            }

            performLookup(allContacts)
        }
    }

    private fun performLookup(contacts: List<MappedContact>) {
        if (!session.identityService().getUserConsent()) {
            return
        }
        viewModelScope.launch {
            val threePids = contacts.flatMap { contact ->
                contact.emails.map { ThreePid.Email(it.email) }
            }

            val data = try {
                session.identityService().lookUp(threePids)
            } catch (failure: Throwable) {
                Timber.w(failure, "Unable to perform the lookup")

                // Should not happen, but just to be sure
                if (failure is IdentityServiceError.UserConsentNotProvided) {
                    setState {
                        copy(userConsent = false)
                    }
                }
                return@launch
            }

            val unresolvedThreePids = mutableListOf<String>()
            val users: MutableMap<String, User> = data.map {
                var user = session.getUser(it.matrixId)
                user = if (user == null) {
                    unresolvedThreePids.add(it.matrixId)
                    User(it.matrixId, TchapUtils.computeDisplayNameFromUserId(it.matrixId), null)
                } else {
                    user
                }
                it.matrixId to user
            }.toMap().toMutableMap()

            localUsers = users.values.toList()

            updateFilteredContacts()

            unresolvedThreePids.mapNotNull {
                tryOrNull { session.resolveUser(it) }
            }.forEach { user -> users[user.userId] = user }

            localUsers = users.values.toList()

            setState {
                copy(
                        isBoundRetrieved = true
                )
            }

            updateFilteredContacts()
        }
    }

    private fun updateFilteredContacts() = withState { state ->
        val tchapContactList: MutableList<User> = roomSummariesUsers.toMutableList()

        tchapContactList.addAll(localUsers.filter { user ->
            !roomSummariesUsers.any { it.userId == user.userId }
        })

        val filteredUsers = if (state.searchTerm.isNotEmpty()) {
            tchapContactList.filter { it.toMatrixItem().getBestName().contains(state.searchTerm, true) }
        } else {
            tchapContactList
        }

        setState {
            copy(
                    filteredLocalUsers = filteredUsers
            )
        }
    }

    override fun handle(action: TchapContactListAction) {
        when (action) {
            is TchapContactListAction.SearchUsers            -> handleSearchUsers(action.value)
            TchapContactListAction.ClearSearchUsers          -> handleClearSearchUsers()
            is TchapContactListAction.AddPendingSelection    -> handleSelectUser(action)
            is TchapContactListAction.RemovePendingSelection -> handleRemoveSelectedUser(action)
            TchapContactListAction.LoadContacts              -> handleLoadContacts()
            TchapContactListAction.SetUserConsent            -> handleSetUserConsent()
            TchapContactListAction.CancelSearch              -> handleCancelSearch()
            TchapContactListAction.OpenSearch                -> handleOpenSearch()
        }.exhaustive
    }

    private fun handleSearchUsers(searchTerm: String) {
        setState {
            copy(searchTerm = searchTerm)
        }
//        knownUsersSearch.tryEmit(searchTerm)
        directoryUsersSearch.tryEmit(searchTerm)
    }

    private fun handleClearSearchUsers() {
//        knownUsersSearch.accept("")
        directoryUsersSearch.tryEmit("")
        setState {
            copy(searchTerm = "")
        }
    }

    private fun handleSetUserConsent() {
        session.identityService().setUserConsent(true)

        setState {
            copy(
                    userConsent = session.identityService().getUserConsent()
            )
        }
    }

    private fun handleOpenSearch() {
        _viewEvents.post(TchapContactListViewEvents.OpenSearch)
    }

    private fun handleCancelSearch() {
        _viewEvents.post(TchapContactListViewEvents.CancelSearch)
    }

    private fun observeUsers() = withState { state ->
//        knownUsersSearch
//                .throttleLast(300, TimeUnit.MILLISECONDS)
//                .observeOn(AndroidSchedulers.mainThread())
//                .switchMap {
//                    session.rx().livePagedUsers(it, state.excludedUserIds)
//                }
//                .execute { async ->
//                    copy(knownUsers = async)
//                }

        if (!isExternalUser) {
            directoryUsersSearch
                    .debounce(300)
                    .onEach { search ->
                        executeSearchDirectory(state, search)
                    }.launchIn(viewModelScope)
        }
    }

    private fun observeDMs() {
        session.flow()
                .liveRoomSummaries(
                        roomSummaryQueryParams {
                            this.memberships = listOf(Membership.JOIN)
                            this.activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(null)
                            this.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
                        }
                )
                .onEach { roomSummaries ->
                    val unresolvedThreePids = mutableListOf<String>()
                    val directUserIds = roomSummaries.mapNotNull { roomSummary -> roomSummary.directUserId }

                    if (directUserIds.isNotEmpty()) {
                        viewModelScope.launch {
                            val usersInfo = tryOrNull { session.usersInfoService().getUsersInfo(directUserIds) }
                            val activeUsersIds = usersInfo?.filterNot { it.value.deactivated }?.keys ?: directUserIds
                            val users = activeUsersIds
                                    .map { userId ->
                                        val user = session.getUser(userId) ?: run {
                                            unresolvedThreePids.add(userId)
                                            User(userId, TchapUtils.computeDisplayNameFromUserId(userId), null)
                                        }
                                        userId to user
                                    }
                                    .toMap()
                                    .toMutableMap()

                            roomSummariesUsers = users.values.toList()

                            updateFilteredContacts()

                            unresolvedThreePids.mapNotNull {
                                tryOrNull { session.resolveUser(it) }
                            }.forEach { user -> users[user.userId] = user }

                            roomSummariesUsers = users.values.toList()

                            updateFilteredContacts()
                        }
                    } else {
                        roomSummariesUsers = emptyList()
                        updateFilteredContacts()
                    }
                }
                .launchIn(viewModelScope)
    }

    private suspend fun executeSearchDirectory(state: TchapContactListViewState, search: String) {
        suspend {
            if (search.isBlank()) {
                emptyList()
            } else {
                val searchResult = session.searchUsersDirectory(search, 50, state.excludedUserIds.orEmpty())
                val userProfile = if (MatrixPatterns.isUserId(search)) {
                    val json = tryOrNull { session.getProfile(search) }
                    User(
                            userId = search,
                            displayName = json?.get(ProfileService.DISPLAY_NAME_KEY) as? String,
                            avatarUrl = json?.get(ProfileService.AVATAR_URL_KEY) as? String
                    )
                } else {
                    null
                }
                if (userProfile == null || searchResult.any { it.userId == userProfile.userId }) {
                    searchResult
                } else {
                    listOf(userProfile) + searchResult
                }
            }
        }.execute {
            copy(directoryUsers = it)
        }
    }

    private fun handleSelectUser(action: TchapContactListAction.AddPendingSelection) = withState { state ->
        val selections = state.pendingSelections.toggle(action.pendingSelection, singleElement = state.singleSelection)
        setState { copy(pendingSelections = selections) }
    }

    private fun handleRemoveSelectedUser(action: TchapContactListAction.RemovePendingSelection) = withState { state ->
        val selections = state.pendingSelections.minus(action.pendingSelection)
        setState { copy(pendingSelections = selections) }
    }
}
