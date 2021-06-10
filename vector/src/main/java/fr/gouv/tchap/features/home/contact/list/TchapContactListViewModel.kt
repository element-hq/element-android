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
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
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

//private typealias KnownUsersSearch = String
private typealias DirectoryUsersSearch = String

class TchapContactListViewModel @AssistedInject constructor(@Assisted initialState: TchapContactListViewState,
                                                            private val contactsDataSource: ContactsDataSource,
                                                            private val session: Session)
    : VectorViewModel<TchapContactListViewState, TchapContactListAction, EmptyViewEvents>(initialState) {

    //    private val knownUsersSearch = BehaviorRelay.create<KnownUsersSearch>()
    private val directoryUsersSearch = BehaviorRelay.create<DirectoryUsersSearch>()

    @AssistedFactory
    interface Factory {
        fun create(initialState: TchapContactListViewState): TchapContactListViewModel
    }

    companion object : MvRxViewModelFactory<TchapContactListViewModel, TchapContactListViewState> {

        override fun create(viewModelContext: ViewModelContext, state: TchapContactListViewState): TchapContactListViewModel? {
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

        selectSubscribe(TchapContactListViewState::searchTerm) { _ ->
            updateFilteredContacts()
        }
    }

    private fun loadContacts() {
        setState {
            copy(
                    mappedContacts = Loading(),
                    identityServerUrl = session.identityService().getCurrentIdentityServerUrl(),
                    userConsent = session.identityService().getUserConsent()
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
            updateFilteredContacts()
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
            val users = mutableMapOf<String, User>()

            data.associateByTo(users,
                    { it.matrixId },
                    {
                        session.getUser(it.matrixId) ?: run {
                            unresolvedThreePids.add(it.matrixId)
                            //Create a temporary user
                            User(it.matrixId, TchapUtils.computeDisplayNameFromUserId(it.matrixId), null)
                        }
                    })

            setState {
                copy(localUsers = users.values.toList())
            }

            updateFilteredContacts()

            unresolvedThreePids.mapNotNull {
                tryOrNull { session.resolveUser(it) }
            }.forEach { user -> users[user.userId] = user }

            setState {
                copy(
                        localUsers = users.values.toList(),
                        isBoundRetrieved = true
                )
            }

            updateFilteredContacts()
        }
    }

    private fun updateFilteredContacts() = withState { state ->
        val filteredUsers = state.localUsers
                .filter { it.getBestName().contains(state.searchTerm, true) }

        val filteredRoomSummaries = state.roomSummaries.invoke()
                ?.filter { it.displayName.contains(state.searchTerm, true) }
                ?.filter { roomSummary -> roomSummary.directUserId != null }
                .orEmpty()

        setState {
            copy(
                    filteredLocalUsers = filteredUsers,
                    filteredRoomSummaries = filteredRoomSummaries
            )
        }
    }

    override fun handle(action: TchapContactListAction) {
        when (action) {
            is TchapContactListAction.SearchUsers -> handleSearchUsers(action.value)
            TchapContactListAction.ClearSearchUsers -> handleClearSearchUsers()
            TchapContactListAction.LoadContacts -> loadContacts()
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
                        Single.just(emptyList<User>())
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
                .observeOn(Schedulers.computation())
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .execute {
                    copy(
                            roomSummaries = it,
                            filteredRoomSummaries = it.invoke().orEmpty()
                    )
                }
    }
}
