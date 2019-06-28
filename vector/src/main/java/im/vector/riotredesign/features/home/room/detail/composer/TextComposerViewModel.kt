/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.features.home.room.detail.composer

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
import im.vector.riotredesign.core.platform.VectorViewModel
import im.vector.riotredesign.features.home.room.detail.RoomDetailFragment
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit

typealias AutocompleteUserQuery = CharSequence

class TextComposerViewModel @AssistedInject constructor(@Assisted initialState: TextComposerViewState,
                                                        private val session: Session
) : VectorViewModel<TextComposerViewState>(initialState) {


    private val room = session.getRoom(initialState.roomId)!!
    private val roomId = initialState.roomId

    private val usersQueryObservable = BehaviorRelay.create<Option<AutocompleteUserQuery>>()

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: TextComposerViewState): TextComposerViewModel
    }

    companion object : MvRxViewModelFactory<TextComposerViewModel, TextComposerViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: TextComposerViewState): TextComposerViewModel? {
            val fragment : RoomDetailFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.textComposerViewModelFactory.create(state)
        }
    }

    init {
        observeUsersQuery()
    }

    fun process(action: TextComposerActions) {
        when (action) {
            is TextComposerActions.QueryUsers -> handleQueryUsers(action)
        }
    }

    private fun handleQueryUsers(action: TextComposerActions.QueryUsers) {
        val query = Option.fromNullable(action.query)
        usersQueryObservable.accept(query)
    }

    private fun observeUsersQuery() {
        Observable.combineLatest<List<String>, Option<AutocompleteUserQuery>, List<User>>(
                room.rx().liveRoomMemberIds(),
                usersQueryObservable.throttleLast(300, TimeUnit.MILLISECONDS),
                BiFunction { roomMembers, query ->
                    val users = roomMembers
                            .mapNotNull {
                                session.getUser(it)
                            }

                    val filter = query.orNull()
                    if (filter.isNullOrBlank()) {
                        users
                    } else {
                        users.filter {
                            it.displayName?.startsWith(prefix = filter, ignoreCase = true)
                            ?: false
                        }
                    }
                }
        ).execute { async ->
            copy(
                    asyncUsers = async
            )
        }

    }
}