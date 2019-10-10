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

package im.vector.riotx.features.home.room.detail.timeline.item

import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotx.R

@EpoxyModelClass
abstract class RedactedMessageItem(useBubble: Boolean) : AbsMessageItem<RedactedMessageItem.Holder>(useBubble) {

    override fun getDefaultLayout(): Int {
        return if (useBubble) {
            if (outgoing) {
                R.layout.item_timeline_event_bubbled_base
            } else {
                R.layout.item_timeline_event_bubbled_base_incoming
            }
        } else R.layout.item_timeline_event_base
    }

    override fun shouldShowReactionAtBottom() = false

    class Holder : AbsMessageItem.Holder(STUB_ID)

    override fun getViewType(): Int {
        // mmm how to do that
        return STUB_ID + if (outgoing) 1000 else 0
    }

    companion object {
        private const val STUB_ID = R.id.messageContentRedactedStub
    }
}
