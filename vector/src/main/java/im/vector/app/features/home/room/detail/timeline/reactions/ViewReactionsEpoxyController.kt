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

package im.vector.app.features.home.room.detail.timeline.reactions

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.EmojiSpanify
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericLoaderItem
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import javax.inject.Inject

/**
 * Epoxy controller for reaction event list
 */
class ViewReactionsEpoxyController @Inject constructor(
        private val stringProvider: StringProvider,
        private val emojiSpanify: EmojiSpanify) :
        TypedEpoxyController<DisplayReactionsViewState>() {

    var listener: Listener? = null

    override fun buildModels(state: DisplayReactionsViewState) {
        val host = this
        when (state.mapReactionKeyToMemberList) {
            Uninitialized,
            is Loading -> {
                genericLoaderItem {
                    id("Spinner")
                }
            }
            is Fail    -> {
                genericFooterItem {
                    id("failure")
                    text(host.stringProvider.getString(R.string.unknown_error).toEpoxyCharSequence())
                }
            }
            is Success -> {
                state.mapReactionKeyToMemberList()?.forEach { reactionInfo ->
                    reactionInfoSimpleItem {
                        id(reactionInfo.eventId)
                        timeStamp(reactionInfo.timestamp)
                        reactionKey(host.emojiSpanify.spanify(reactionInfo.reactionKey).toEpoxyCharSequence())
                        authorDisplayName(reactionInfo.authorName ?: reactionInfo.authorId)
                        userClicked { host.listener?.didSelectUser(reactionInfo.authorId) }
                    }
                }
            }
        }
    }

    interface Listener {
        fun didSelectUser(userId: String)
    }
}
