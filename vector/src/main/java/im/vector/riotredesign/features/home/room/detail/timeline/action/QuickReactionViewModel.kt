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
enum class TriggleState {
    NONE,
    FIRST,
    SECOND
}

data class QuickReactionState(
        val roomId: String,
        val eventId: String,
        val informationData: MessageInformationData,
        val agreeTriggleState: TriggleState = TriggleState.NONE,
        val likeTriggleState: TriggleState = TriggleState.NONE,
        /** Pair of 'clickedOn' and current toggles state*/
        val selectionResult: Pair<String, List<String>>? = null
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

        const val AGREE_POSITIVE = "👍"
        const val AGREE_NEGATIVE = "👎"
        const val LIKE_POSITIVE = "🙂"
        const val LIKE_NEGATIVE = "😔"

        fun getOpposite(reaction: String): String? {
            return when (reaction) {
                AGREE_POSITIVE -> AGREE_NEGATIVE
                AGREE_NEGATIVE -> AGREE_POSITIVE
                LIKE_POSITIVE  -> LIKE_NEGATIVE
                LIKE_NEGATIVE  -> LIKE_POSITIVE
                else           -> null
            }
        }

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
        var agreeTriggle: TriggleState = TriggleState.NONE
        var likeTriggle: TriggleState = TriggleState.NONE
        event.annotations?.reactionsSummary?.forEach {
            //it.addedByMe
            if (it.addedByMe) {
                if (AGREE_POSITIVE == it.key) {
                    agreeTriggle = TriggleState.FIRST
                } else if (AGREE_NEGATIVE == it.key) {
                    agreeTriggle = TriggleState.SECOND
                }

                if (LIKE_POSITIVE == it.key) {
                    likeTriggle = TriggleState.FIRST
                } else if (LIKE_NEGATIVE == it.key) {
                    likeTriggle = TriggleState.SECOND
                }
            }
        }
        return state.copy(
                agreeTriggleState = agreeTriggle,
                likeTriggleState = likeTriggle
        )
    }

    fun toggleAgree(isFirst: Boolean) = withState {
        if (isFirst) {
            setState {
                val newTriggle = if (it.agreeTriggleState == TriggleState.FIRST) TriggleState.NONE else TriggleState.FIRST
                copy(
                        agreeTriggleState = newTriggle,
                        selectionResult = Pair(AGREE_POSITIVE, getReactions(this, newTriggle, null))
                )
            }
        } else {
            setState {
                val newTriggle = if (it.agreeTriggleState == TriggleState.SECOND) TriggleState.NONE else TriggleState.SECOND
                copy(
                        agreeTriggleState = agreeTriggleState,
                        selectionResult = Pair(AGREE_NEGATIVE, getReactions(this, newTriggle, null))
                )
            }
        }
    }

    fun toggleLike(isFirst: Boolean) = withState {
        if (isFirst) {
            setState {
                val newTriggle = if (it.likeTriggleState == TriggleState.FIRST) TriggleState.NONE else TriggleState.FIRST
                copy(
                        likeTriggleState = newTriggle,
                        selectionResult = Pair(LIKE_POSITIVE, getReactions(this, null, newTriggle))
                )
            }
        } else {
            setState {
                val newTriggle = if (it.likeTriggleState == TriggleState.SECOND) TriggleState.NONE else TriggleState.SECOND
                copy(
                        likeTriggleState = newTriggle,
                        selectionResult = Pair(LIKE_NEGATIVE, getReactions(this, null, newTriggle))
                )
            }
        }
    }

    private fun getReactions(state: QuickReactionState, newState1: TriggleState?, newState2: TriggleState?): List<String> {
        return ArrayList<String>(4).apply {
            when (newState2 ?: state.likeTriggleState) {
                TriggleState.FIRST  -> add(LIKE_POSITIVE)
                TriggleState.SECOND -> add(LIKE_NEGATIVE)
                else                -> {
                }
            }
            when (newState1 ?: state.agreeTriggleState) {
                TriggleState.FIRST  -> add(AGREE_POSITIVE)
                TriggleState.SECOND -> add(AGREE_NEGATIVE)
                else                -> {
                }
            }
        }
    }
}