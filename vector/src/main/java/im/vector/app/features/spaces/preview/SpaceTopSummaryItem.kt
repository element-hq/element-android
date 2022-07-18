/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.spaces.preview

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.extensions.setTextOrHide

@EpoxyModelClass
abstract class SpaceTopSummaryItem : VectorEpoxyModel<SpaceTopSummaryItem.Holder>(R.layout.item_space_top_summary) {

    @EpoxyAttribute
    var topic: String? = null

    @EpoxyAttribute
    lateinit var formattedMemberCount: String

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.spaceTopicText.setTextOrHide(topic)
        holder.memberCountText.text = formattedMemberCount
    }

    class Holder : VectorEpoxyHolder() {
        val memberCountText by bind<TextView>(R.id.spaceSummaryMemberCountText)
        val spaceTopicText by bind<TextView>(R.id.spaceSummaryTopic)
    }
}
