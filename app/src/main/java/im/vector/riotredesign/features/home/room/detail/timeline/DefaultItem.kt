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

package im.vector.riotredesign.features.home.room.detail.timeline

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.RiotEpoxyHolder
import im.vector.riotredesign.core.epoxy.RiotEpoxyModel

@EpoxyModelClass(layout = R.layout.item_timeline_event_default)
abstract class DefaultItem : RiotEpoxyModel<DefaultItem.Holder>() {

    @EpoxyAttribute var text: CharSequence? = null

    override fun bind(holder: Holder) {
        holder.messageView.text = text
    }

    class Holder : RiotEpoxyHolder() {
        val messageView by bind<TextView>(R.id.stateMessageView)
    }
}