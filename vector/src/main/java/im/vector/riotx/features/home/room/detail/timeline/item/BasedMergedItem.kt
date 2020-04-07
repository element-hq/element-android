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

package im.vector.riotx.features.home.room.detail.timeline.item

import androidx.annotation.IdRes
import com.airbnb.epoxy.EpoxyAttribute
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController

abstract class BasedMergedItem<H : BasedMergedItem.Holder> : BaseEventItem<H>() {

    @EpoxyAttribute
    lateinit var attributes: Attributes

    protected val distinctMergeData by lazy {
        attributes.mergeData.distinctBy { it.userId }
    }

    override fun getEventIds(): List<String> {
        return if (attributes.isCollapsed) {
            attributes.mergeData.map { it.eventId }
        } else {
            emptyList()
        }
    }

    data class Data(
            val localId: Long,
            val eventId: String,
            val userId: String,
            val memberName: String,
            val avatarUrl: String?
    )

    fun Data.toMatrixItem() = MatrixItem.UserItem(userId, memberName, avatarUrl)

    data class Attributes(
            val isCollapsed: Boolean,
            val mergeData: List<Data>,
            val avatarRenderer: AvatarRenderer,
            val readReceiptsCallback: TimelineEventController.ReadReceiptsCallback? = null,
            val onCollapsedStateChanged: (Boolean) -> Unit
    )

    abstract class Holder(@IdRes stubId: Int) : BaseEventItem.BaseHolder(stubId) {
        //val reactionsContainer by bind<ViewGroup>(R.id.reactionsContainer)
    }
}
