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

package im.vector.riotx.features.home.room.detail.timeline.action

import android.content.Context
import android.graphics.Typeface
import android.text.format.DateUtils
import androidx.emoji.text.EmojiCompat
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import im.vector.riotx.EmojiCompatHelper
import im.vector.riotx.R
import im.vector.riotx.core.ui.list.genericFooterItem
import im.vector.riotx.core.ui.list.genericLoaderItem

/**
 * Epoxy controller for reaction event list
 */
class ViewReactionsEpoxyController(private val context: Context)
    : TypedEpoxyController<DisplayReactionsViewState>() {

    override fun buildModels(state: DisplayReactionsViewState) {
        when (state.mapReactionKeyToMemberList) {
            is Incomplete -> {
                genericLoaderItem {
                    id("Spinner")
                }
            }
            is Fail       -> {
                genericFooterItem {
                    id("failure")
                    text(context.getString(R.string.unknown_error))
                }
            }
            is Success    -> {
                state.mapReactionKeyToMemberList()?.forEach {
                    reactionInfoSimpleItem {
                        id(it.eventId)
                        timeStamp(it.timestamp)
                        reactionKey(EmojiCompatHelper.safeEmojiSpanify(it.reactionKey))
                        authorDisplayName(it.authorName ?: it.authorId)
                    }
                }
            }
        }

    }
}