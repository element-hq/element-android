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

package im.vector.riotx.features.share

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.rx.rx
import im.vector.riotx.ActiveSessionDataSource
import im.vector.riotx.core.platform.EmptyAction
import im.vector.riotx.core.platform.VectorViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

data class IncomingShareState(private val dummy: Boolean = false) : MvRxState

/**
 * View model used to observe the room list and post update to the ShareRoomListObservableStore
 */
class IncomingShareViewModel @AssistedInject constructor(@Assisted initialState: IncomingShareState,
                                                         private val sessionObservableStore: ActiveSessionDataSource,
                                                         private val shareRoomListObservableStore: ShareRoomListDataSource)
    : VectorViewModel<IncomingShareState, EmptyAction>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: IncomingShareState): IncomingShareViewModel
    }

    companion object : MvRxViewModelFactory<IncomingShareViewModel, IncomingShareState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: IncomingShareState): IncomingShareViewModel? {
            val activity: IncomingShareActivity = (viewModelContext as ActivityViewModelContext).activity()
            return activity.incomingShareViewModelFactory.create(state)
        }
    }

    init {
        observeRoomSummaries()
    }

    private fun observeRoomSummaries() {
        sessionObservableStore.observe()
                .observeOn(AndroidSchedulers.mainThread())
                .switchMap {
                    it.orNull()?.rx()?.liveRoomSummaries()
                            ?: Observable.just(emptyList())
                }
                .throttleLast(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    shareRoomListObservableStore.post(it)
                }
                .disposeOnClear()
    }

    override fun handle(action: EmptyAction) {
        // No op
    }
}
