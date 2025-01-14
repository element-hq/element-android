/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.features.home.AvatarRenderer

@EpoxyModelClass
abstract class DefaultItem : BaseEventItem<DefaultItem.Holder>(R.layout.item_timeline_event_base_noinfo) {

    @EpoxyAttribute
    lateinit var attributes: Attributes

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.messageTextView.text = attributes.text
        attributes.avatarRenderer.render(attributes.informationData.matrixItem, holder.avatarImageView)
        holder.view.setOnLongClickListener(attributes.itemLongClickListener)
    }

    override fun unbind(holder: Holder) {
        attributes.avatarRenderer.clear(holder.avatarImageView)
        super.unbind(holder)
    }

    override fun getEventIds(): List<String> {
        return listOf(attributes.informationData.eventId)
    }

    override fun getViewStubId() = STUB_ID

    class Holder : BaseHolder(STUB_ID) {
        val avatarImageView by bind<ImageView>(R.id.itemDefaultAvatarView)
        val messageTextView by bind<TextView>(R.id.itemDefaultTextView)
    }

    data class Attributes(
            val avatarRenderer: AvatarRenderer,
            val informationData: MessageInformationData,
            val text: String,
            val itemLongClickListener: View.OnLongClickListener? = null
    )

    companion object {
        private val STUB_ID = R.id.messageContentDefaultStub
    }
}
