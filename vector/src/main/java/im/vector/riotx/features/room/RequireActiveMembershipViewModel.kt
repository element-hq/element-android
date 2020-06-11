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

package im.vector.riotx.features.room

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.rx.rx
import im.vector.matrix.rx.unwrap
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorViewModel
import io.reactivex.disposables.Disposable
import timber.log.Timber

/**
 * This ViewModel observe a room summary and notify when the room is left
 */
class RequireActiveMembershipViewModel @AssistedInject constructor(
        @Assisted initialState: RequireActiveMembershipViewState,
        private val session: Session)
    : VectorViewModel<RequireActiveMembershipViewState, RequireActiveMembershipAction, RequireActiveMembershipViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: RequireActiveMembershipViewState): RequireActiveMembershipViewModel
    }

    companion object : MvRxViewModelFactory<RequireActiveMembershipViewModel, RequireActiveMembershipViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RequireActiveMembershipViewState): RequireActiveMembershipViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    private var currentDisposable: Disposable? = null

    init {
        observeRoomSummary(initialState.roomId)
    }

    private fun observeRoomSummary(roomId: String?) {
        currentDisposable?.dispose()

        currentDisposable = roomId
                ?.let { session.getRoom(it) }
                ?.let { room ->
                    room.rx().liveRoomSummary()
                            .unwrap()
                            .subscribe {
                                if (it.membership.isLeft()) {
                                    Timber.w("The room has been left")
                                    _viewEvents.post(RequireActiveMembershipViewEvents.RoomLeft)
                                }
                            }
                }
    }

    override fun onCleared() {
        super.onCleared()
        currentDisposable?.dispose()
    }

    override fun handle(action: RequireActiveMembershipAction) {
        when (action) {
            is RequireActiveMembershipAction.ChangeRoom -> {
                setState {
                    copy(roomId = action.roomId)
                }
                observeRoomSummary(action.roomId)
            }
        }.exhaustive
    }
}
