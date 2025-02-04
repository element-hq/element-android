/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.reactions

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.EmojiSpanify
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericLoaderItem
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

/**
 * Epoxy controller for reaction event list.
 */
class ViewReactionsEpoxyController @Inject constructor(
        private val stringProvider: StringProvider,
        private val emojiSpanify: EmojiSpanify
) :
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
            is Fail -> {
                genericFooterItem {
                    id("failure")
                    text(host.stringProvider.getString(CommonStrings.unknown_error).toEpoxyCharSequence())
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
