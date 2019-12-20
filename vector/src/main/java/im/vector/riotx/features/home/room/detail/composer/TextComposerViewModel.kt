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

package im.vector.riotx.features.home.room.detail.composer

import arrow.core.Option
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.jakewharton.rxrelay2.BehaviorRelay
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.user.model.User
import im.vector.matrix.rx.rx
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.home.room.detail.RoomDetailFragment
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit

typealias AutocompleteQuery = CharSequence

class TextComposerViewModel @AssistedInject constructor(@Assisted initialState: TextComposerViewState,
                                                        private val session: Session
) : VectorViewModel<TextComposerViewState, TextComposerAction>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!

    private val usersQueryObservable = BehaviorRelay.create<Option<AutocompleteQuery>>()
    private val roomsQueryObservable = BehaviorRelay.create<Option<AutocompleteQuery>>()
    private val groupsQueryObservable = BehaviorRelay.create<Option<AutocompleteQuery>>()

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: TextComposerViewState): TextComposerViewModel
    }

    companion object : MvRxViewModelFactory<TextComposerViewModel, TextComposerViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: TextComposerViewState): TextComposerViewModel? {
            val fragment: RoomDetailFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.textComposerViewModelFactory.create(state)
        }
    }

    init {
        observeUsersQuery()
        observeRoomsQuery()
        observeGroupsQuery()
    }

    override fun handle(action: TextComposerAction) {
        when (action) {
            is TextComposerAction.QueryUsers  -> handleQueryUsers(action)
            is TextComposerAction.QueryRooms  -> handleQueryRooms(action)
            is TextComposerAction.QueryGroups -> handleQueryGroups(action)
        }
    }

    private fun handleQueryUsers(action: TextComposerAction.QueryUsers) {
        val query = Option.fromNullable(action.query)
        usersQueryObservable.accept(query)
    }


    private fun handleQueryRooms(action: TextComposerAction.QueryRooms) {
        val query = Option.fromNullable(action.query)
        roomsQueryObservable.accept(query)
    }

    private fun handleQueryGroups(action: TextComposerAction.QueryGroups) {
        val query = Option.fromNullable(action.query)
        groupsQueryObservable.accept(query)
    }

    private fun observeUsersQuery() {
        Observable.combineLatest<List<String>, Option<AutocompleteQuery>, List<User>>(
                room.rx().liveRoomMemberIds(),
                usersQueryObservable.throttleLast(300, TimeUnit.MILLISECONDS),
                BiFunction { roomMemberIds, query ->
                    val users = roomMemberIds.mapNotNull { session.getUser(it) }

                    val filter = query.orNull()
                    if (filter.isNullOrBlank()) {
                        users
                    } else {
                        users.filter {
                            it.displayName?.startsWith(prefix = filter, ignoreCase = true) ?: false
                        }
                    }
                            .sortedBy { it.displayName }
                }
        ).execute { async ->
            copy(
                    asyncUsers = async
            )
        }
    }

    private fun observeRoomsQuery() {
        Observable.combineLatest<List<RoomSummary>, Option<AutocompleteQuery>, List<RoomSummary>>(
                session.rx().liveRoomSummaries(),
                roomsQueryObservable.throttleLast(300, TimeUnit.MILLISECONDS),
                BiFunction { roomSummaries, query ->
                    val filter = query.orNull() ?: ""
                    // Keep only room with a canonical alias
                    roomSummaries
                            .filter {
                                it.canonicalAlias?.contains(filter, ignoreCase = true) == true
                            }
                            .sortedBy { it.displayName }
                }
        ).execute { async ->
            copy(
                    asyncRooms = async
            )
        }
    }

    private fun observeGroupsQuery() {
        Observable.combineLatest<List<GroupSummary>, Option<AutocompleteQuery>, List<GroupSummary>>(
                session.rx().liveGroupSummaries(),
                groupsQueryObservable.throttleLast(300, TimeUnit.MILLISECONDS),
                BiFunction { groupSummaries, query ->
                    val filter = query.orNull() ?: ""
                    groupSummaries
                            .filter {
                                it.groupId.contains(filter, ignoreCase = true)
                            }
                            .sortedBy { it.displayName }
                }
        ).execute { async ->
            copy(
                    asyncGroups = async
            )
        }
    }
}
