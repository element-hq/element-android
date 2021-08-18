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

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import fr.gouv.tchap.core.utils.TchapUtils
import im.vector.app.core.contacts.ContactsDataSource
import im.vector.app.core.contacts.MappedContact
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.toggle
import im.vector.app.core.platform.VectorViewModel
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.query.ActiveSpaceFilter
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.rx.asObservable
import org.matrix.android.sdk.rx.rx
import timber.log.Timber
import java.util.concurrent.TimeUnit

// private typealias KnownUsersSearch = String
private typealias DirectoryUsersSearch = String

class TchapContactListViewModel @AssistedInject constructor(@Assisted initialState: TchapContactListViewState,
                                                            private val contactsDataSource: ContactsDataSource,
                                                            private val session: Session)
    : VectorViewModel<TchapContactListViewState, TchapContactListAction, TchapContactListViewEvents>(initialState) {

    //    private val knownUsersSearch = BehaviorRelay.create<KnownUsersSearch>()
    private val directoryUsersSearch = BehaviorRelay.create<DirectoryUsersSearch>()

    // All users from roomSummaries
    private var roomSummariesUsers: List<User> = emptyList()

    // All the contacts on the phone
    private var localUsers: List<User> = emptyList()

    @AssistedFactory
    interface Factory {
        fun create(initialState: TchapContactListViewState): TchapContactListViewModel
    }

    companion object : MvRxViewModelFactory<TchapContactListViewModel, TchapContactListViewState> {

        override fun initialState(viewModelContext: ViewModelContext): TchapContactListViewState {
            return TchapContactListViewState(
                    excludedUserIds = null,
                    singleSelection = true,
                    showSearch = true,
                    showInviteActions = false
            )
        }

        override fun create(viewModelContext: ViewModelContext, state: TchapContactListViewState): TchapContactListViewModel {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    init {
        observeUsers()
        observeDMs()

        selectSubscribe(TchapContactListViewState::searchTerm) {
            updateFilteredContacts()
        }

        setState {
            copy(
                    identityServerUrl = session.identityService().getCurrentIdentityServerUrl(),
                    userConsent = session.identityService().getUserConsent()
            )
        }

        if (!TchapUtils.isExternalTchapUser(session.myUserId)) {
            setState {
                copy(
                        showInviteActions = true
                )
            }
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
            tchapContactList.filter { it.getBestName().contains(state.searchTerm, true) }
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
//        knownUsersSearch.accept(searchTerm)
        directoryUsersSearch.accept(searchTerm)
    }

    private fun handleClearSearchUsers() {
//        knownUsersSearch.accept("")
        directoryUsersSearch.accept("")
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

        directoryUsersSearch
                .debounce(300, TimeUnit.MILLISECONDS)
                .switchMapSingle { search ->
                    val stream = if (search.isBlank()) {
                        Single.just(emptyList())
                    } else {
                        session.rx().searchUsersDirectory(search, 50, state.excludedUserIds.orEmpty())
                    }
                    stream.toAsync {
                        copy(directoryUsers = it)
                    }
                }
                .subscribe()
                .disposeOnClear()
    }

    private fun observeDMs() {
        session.getRoomSummariesLive(
                roomSummaryQueryParams {
                    this.memberships = listOf(Membership.JOIN)
                    this.activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(null)
                    this.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
                }
        ).asObservable()
                .subscribe { roomSummaries ->
                    val unresolvedThreePids = mutableListOf<String>()
                    val directUserIds = roomSummaries.mapNotNull { roomSummary -> roomSummary.directUserId }

                    viewModelScope.launch {
                        val activeUsersIds = try {
                            session.usersInfoService()
                                    .getUsersInfo(directUserIds)
                                    .filterNot { it.value.deactivated }
                                    .keys
                        } catch (failure: Throwable) {
                            if (failure is Failure.NetworkConnection
                                    || (failure is Failure.ServerError && failure.error.code == MatrixError.ORG_MATRIX_EXPIRED_ACCOUNT)) {
                                // Todo: maybe add a retry mechanism
                                emptySet()
                            } else {
                                // Other error, propagate it
                                throw failure
                            }
                        }
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
                }.disposeOnClear()
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
