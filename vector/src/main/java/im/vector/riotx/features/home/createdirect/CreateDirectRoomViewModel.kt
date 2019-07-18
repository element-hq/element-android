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

import arrow.core.Option
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.jakewharton.rxrelay2.BehaviorRelay
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.user.model.User
import im.vector.matrix.rx.rx
import im.vector.riotx.core.platform.VectorViewModel
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

private typealias KnowUsersFilter = String

class CreateDirectRoomViewModel @AssistedInject constructor(@Assisted
                                                            initialState: CreateDirectRoomViewState,
                                                            private val session: Session)
    : VectorViewModel<CreateDirectRoomViewState>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: CreateDirectRoomViewState): CreateDirectRoomViewModel
    }

    private val knownUsersFilter = BehaviorRelay.createDefault<Option<KnowUsersFilter>>(Option.empty())

    companion object : MvRxViewModelFactory<CreateDirectRoomViewModel, CreateDirectRoomViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: CreateDirectRoomViewState): CreateDirectRoomViewModel? {
            val fragment: CreateDirectRoomFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.createDirectRoomViewModelFactory.create(state)
        }
    }

    init {
        observeKnownUsers()
    }

    fun handle(createDirectRoomActions: CreateDirectRoomActions) {
        when (createDirectRoomActions) {
            is CreateDirectRoomActions.CreateRoomAndInviteSelectedUsers -> createRoomAndInviteSelectedUsers()
            is CreateDirectRoomActions.SelectAddByMatrixId              -> handleSelectAddByMatrixId()
            is CreateDirectRoomActions.FilterKnownUsers                 -> knownUsersFilter.accept(Option.just(createDirectRoomActions.value))
            is CreateDirectRoomActions.ClearFilterKnownUsers            -> knownUsersFilter.accept(Option.empty())
        }
    }

    private fun handleSelectAddByMatrixId() {
        // TODO
    }

    private fun createRoomAndInviteSelectedUsers() {
        // TODO
    }

    private fun observeKnownUsers() {
        Observable
                .combineLatest<List<User>, Option<KnowUsersFilter>, List<User>>(
                        session.rx().liveUsers(),
                        knownUsersFilter.throttleLast(300, TimeUnit.MILLISECONDS),
                        BiFunction { users, filter ->
                            val filterValue = filter.orNull()
                            if (filterValue.isNullOrEmpty()) {
                                users
                            } else {
                                users.filter {
                                    it.displayName?.contains(filterValue, ignoreCase = true) ?: false
                                    || it.userId.contains(filterValue, ignoreCase = true)
                                }
                            }
                        }
                ).execute { async ->
                    copy(knownUsers = async)
                }
    }

}