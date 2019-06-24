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

import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import im.vector.matrix.android.api.session.Session
import im.vector.riotredesign.core.platform.VectorViewModel
import org.koin.android.ext.android.get

/**
 * Quick reactions state, it's a toggle with 3rd state
 */
data class ToggleState(
        val reaction: String,
        val isSelected: Boolean
)

data class QuickReactionState(
        val quickStates: List<ToggleState>,
        val eventId: String = "",
        val result: ToggleState? = null
) : MvRxState

/**
 * Quick reaction view model
 */
class QuickReactionViewModel(initialState: QuickReactionState) : VectorViewModel<QuickReactionState>(initialState) {


    fun didSelect(index: Int) = withState {
        val isSelected = it.quickStates[index].isSelected
        setState {
            copy(result = ToggleState(it.quickStates[index].reaction, !isSelected))
        }
    }

    companion object : MvRxViewModelFactory<QuickReactionViewModel, QuickReactionState> {

        val quickEmojis = listOf("👍", "👎", "😄", "🎉", "😕", "❤️", "🚀", "👀")

        override fun initialState(viewModelContext: ViewModelContext): QuickReactionState? {
            val currentSession = viewModelContext.activity.get<Session>()
            val parcel = viewModelContext.args as TimelineEventFragmentArgs
            val event = currentSession.getRoom(parcel.roomId)?.getTimeLineEvent(parcel.eventId)
                    ?: return null

            val summary = event.annotations?.reactionsSummary
            val quickReactions = quickEmojis.map { emoji ->
                ToggleState(emoji, summary?.firstOrNull { it.key == emoji }?.addedByMe ?: false)
            }
            return QuickReactionState(quickReactions, event.root.eventId ?: "")
        }
    }
}