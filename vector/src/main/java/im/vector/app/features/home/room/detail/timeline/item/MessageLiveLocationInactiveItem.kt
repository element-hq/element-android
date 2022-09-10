/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.item

import android.widget.ImageView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R

@EpoxyModelClass
abstract class MessageLiveLocationInactiveItem :
        AbsMessageItem<MessageLiveLocationInactiveItem.Holder>(),
        LiveLocationShareStatusItem by DefaultLiveLocationShareStatusItem() {

    @EpoxyAttribute
    var mapWidth: Int = 0

    @EpoxyAttribute
    var mapHeight: Int = 0

    override fun bind(holder: Holder) {
        super.bind(holder)
        renderSendState(holder.view, null)
        bindMap(holder.noLocationMapImageView, mapWidth, mapHeight, attributes.informationData.messageLayout)
        bindBottomBanner(holder.bannerImageView, attributes.informationData.messageLayout)
    }

    override fun getViewStubId() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val bannerImageView by bind<ImageView>(R.id.liveLocationEndedBannerBackground)
        val noLocationMapImageView by bind<ImageView>(R.id.liveLocationInactiveMap)
    }

    companion object {
        private val STUB_ID = R.id.messageContentLiveLocationInactiveStub
    }
}
