/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
