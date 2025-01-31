/*
 * Copyright 2019-2025 New Vector Ltd.
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
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.ui.views.ShieldImageView
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

@EpoxyModelClass
abstract class NoticeItem : BaseEventItem<NoticeItem.Holder>(R.layout.item_timeline_event_base_noinfo) {

    @EpoxyAttribute
    lateinit var attributes: Attributes

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.noticeTextView.text = attributes.noticeText.charSequence
        attributes.avatarRenderer.render(attributes.informationData.matrixItem, holder.avatarImageView)
        holder.view.setOnLongClickListener(attributes.itemLongClickListener)
        holder.avatarImageView.onClick(attributes.avatarClickListener)

        when (attributes.informationData.e2eDecoration) {
            E2EDecoration.NONE -> {
                holder.e2EDecorationView.render(null)
            }
            E2EDecoration.WARN_IN_CLEAR,
            E2EDecoration.WARN_SENT_BY_UNVERIFIED,
            E2EDecoration.WARN_SENT_BY_UNKNOWN -> {
                holder.e2EDecorationView.render(RoomEncryptionTrustLevel.Warning)
            }
            E2EDecoration.WARN_SENT_BY_DELETED_SESSION -> TODO()
            E2EDecoration.WARN_UNSAFE_KEY -> TODO()
        }
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
        val avatarImageView by bind<ImageView>(R.id.itemNoticeAvatarView)
        val noticeTextView by bind<TextView>(R.id.itemNoticeTextView)
        val e2EDecorationView by bind<ShieldImageView>(R.id.messageE2EDecoration)
    }

    data class Attributes(
            val avatarRenderer: AvatarRenderer,
            val informationData: MessageInformationData,
            val noticeText: EpoxyCharSequence,
            val itemLongClickListener: View.OnLongClickListener? = null,
            val readReceiptsCallback: TimelineEventController.ReadReceiptsCallback? = null,
            val avatarClickListener: ClickListener? = null,
            val threadSummaryClickListener: ClickListener? = null
    )

    companion object {
        private val STUB_ID = R.id.messageContentNoticeStub
    }
}
