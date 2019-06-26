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
package im.vector.riotredesign.features.home.room.detail.timeline.action

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.riotredesign.core.platform.VectorViewModel
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageInformationData

/**
 * Quick reactions state, it's a toggle with 3rd state
 */
data class ToggleState(
        val reaction: String,
        val isSelected: Boolean
)

data class QuickReactionState(
        val roomId: String,
        val eventId: String,
        val informationData: MessageInformationData,
        val quickStates: List<ToggleState> = emptyList(),
        val result: ToggleState? = null
        /** Pair of 'clickedOn' and current toggles state*/
) : MvRxState {

    constructor(args: TimelineEventFragmentArgs) : this(roomId = args.roomId, eventId = args.eventId, informationData = args.informationData)
}

/**
 * Quick reaction view model
 */
class QuickReactionViewModel @AssistedInject constructor(@Assisted initialState: QuickReactionState,
                                                         private val session: Session) : VectorViewModel<QuickReactionState>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: QuickReactionState): QuickReactionViewModel
    }

    companion object : MvRxViewModelFactory<QuickReactionViewModel, QuickReactionState> {

        val quickEmojis = listOf("👍", "👎", "😄", "🎉", "😕", "❤️", "🚀", "👀")

        override fun create(viewModelContext: ViewModelContext, state: QuickReactionState): QuickReactionViewModel? {
            val fragment: QuickReactionFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.quickReactionViewModelFactory.create(state)
        }
    }

    init {
        setState { reduceState(this) }
    }

    private fun reduceState(state: QuickReactionState): QuickReactionState {
        val event = session.getRoom(state.roomId)?.getTimeLineEvent(state.eventId) ?: return state
        val summary = event.annotations?.reactionsSummary
        val quickReactions = quickEmojis.map { emoji ->
            ToggleState(emoji, summary?.firstOrNull { it.key == emoji }?.addedByMe ?: false)
        }
        return state.copy(quickStates = quickReactions)
    }

    fun didSelect(index: Int) = withState {
        val isSelected = it.quickStates[index].isSelected
        setState {
            copy(result = ToggleState(it.quickStates[index].reaction, !isSelected))
        }
    }

}