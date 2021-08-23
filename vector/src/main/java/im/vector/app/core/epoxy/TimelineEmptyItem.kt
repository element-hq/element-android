/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.core.epoxy

import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.features.home.room.detail.timeline.item.ItemWithEvents

@EpoxyModelClass(layout = R.layout.item_timeline_empty)
abstract class TimelineEmptyItem : VectorEpoxyModel<TimelineEmptyItem.Holder>(), ItemWithEvents {

    @EpoxyAttribute lateinit var eventId: String
    @EpoxyAttribute var notBlank: Boolean = false

    override fun isVisible() = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.view.updateLayoutParams {
            // Force height to 1px so scrolling works correctly
            this.height = if (notBlank) 1 else 0
        }
    }

    override fun getEventIds(): List<String> {
        return listOf(eventId)
    }

    class Holder : VectorEpoxyHolder()
}
