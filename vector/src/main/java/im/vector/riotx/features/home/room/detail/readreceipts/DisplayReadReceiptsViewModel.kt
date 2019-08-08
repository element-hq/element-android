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

package im.vector.riotx.features.home.room.detail.readreceipts

import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.rx.RxRoom
import im.vector.riotx.core.platform.VectorViewModel

/**
 * Used to display the list of read receipts to a given event
 */
class DisplayReadReceiptsViewModel @AssistedInject constructor(@Assisted initialState: DisplayReadReceiptsViewState,
                                                               private val session: Session
) : VectorViewModel<DisplayReadReceiptsViewState>(initialState) {

    private val roomId = initialState.roomId
    private val eventId = initialState.eventId
    private val room = session.getRoom(roomId)
                       ?: throw IllegalStateException("Shouldn't use this ViewModel without a room")

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: DisplayReadReceiptsViewState): DisplayReadReceiptsViewModel
    }

    companion object : MvRxViewModelFactory<DisplayReadReceiptsViewModel, DisplayReadReceiptsViewState> {

        override fun create(viewModelContext: ViewModelContext, state: DisplayReadReceiptsViewState): DisplayReadReceiptsViewModel? {
            val fragment: DisplayReadReceiptsBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.displayReadReceiptsViewModelFactory.create(state)
        }
    }

    init {
        observeEventAnnotationSummaries()
    }

    private fun observeEventAnnotationSummaries() {
        RxRoom(room)
                .liveEventReadReceipts(eventId)
                .execute {
                    copy(readReceipts = it)
                }
    }

}