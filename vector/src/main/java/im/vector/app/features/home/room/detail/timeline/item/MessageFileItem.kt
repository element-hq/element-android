/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.features.home.room.detail.timeline.helper.ContentDownloadStateTrackerBinder
import im.vector.app.features.home.room.detail.timeline.helper.ContentUploadStateTrackerBinder
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import im.vector.app.features.themes.ThemeUtils

@EpoxyModelClass
abstract class MessageFileItem : AbsMessageItem<MessageFileItem.Holder>() {

    @EpoxyAttribute
    var filename: String = ""

    @EpoxyAttribute
    var mxcUrl: String = ""

    @EpoxyAttribute
    @DrawableRes
    var iconRes: Int = 0

    @EpoxyAttribute
    var izLocalFile = false

    @EpoxyAttribute
    var izDownloaded = false

    @EpoxyAttribute
    lateinit var contentUploadStateTrackerBinder: ContentUploadStateTrackerBinder

    @EpoxyAttribute
    lateinit var contentDownloadStateTrackerBinder: ContentDownloadStateTrackerBinder

    override fun bind(holder: Holder) {
        super.bind(holder)
        renderSendState(holder.fileLayout, holder.filenameView)

        if (!attributes.informationData.sendState.hasFailed()) {
            contentUploadStateTrackerBinder.bind(attributes.informationData.eventId, izLocalFile, holder.progressLayout)
        } else {
            holder.fileImageView.setImageResource(R.drawable.ic_cross)
            holder.progressLayout.isVisible = false
        }

        holder.filenameView.text = filename

        if (attributes.informationData.sendState.isSending()) {
            holder.fileImageView.setImageResource(iconRes)
        } else {
            if (izDownloaded) {
                holder.fileImageView.setImageResource(iconRes)
                holder.fileDownloadProgress.progress = 0
            } else {
                contentDownloadStateTrackerBinder.bind(mxcUrl, holder)
                holder.fileImageView.setImageResource(R.drawable.ic_download)
            }
        }

        val backgroundTint = if (attributes.informationData.messageLayout is TimelineMessageLayout.Bubble) {
            Color.TRANSPARENT
        } else {
            ThemeUtils.getColor(holder.view.context, im.vector.lib.ui.styles.R.attr.vctr_content_quinary)
        }
        holder.mainLayout.backgroundTintList = ColorStateList.valueOf(backgroundTint)
        holder.filenameView.onClick(attributes.itemClickListener)
        holder.filenameView.setOnLongClickListener(attributes.itemLongClickListener)
        holder.fileImageWrapper.onClick(attributes.itemClickListener)
        holder.fileImageWrapper.setOnLongClickListener(attributes.itemLongClickListener)
        holder.filenameView.paintFlags = (holder.filenameView.paintFlags or Paint.UNDERLINE_TEXT_FLAG)
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        contentUploadStateTrackerBinder.unbind(attributes.informationData.eventId)
        contentDownloadStateTrackerBinder.unbind(mxcUrl)
    }

    override fun getViewStubId() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val mainLayout by bind<ViewGroup>(R.id.messageFileMainLayout)
        val progressLayout by bind<ViewGroup>(R.id.messageFileUploadProgressLayout)
        val fileLayout by bind<ViewGroup>(R.id.messageFileLayout)
        val fileImageView by bind<ImageView>(R.id.messageFileIconView)
        val fileImageWrapper by bind<ViewGroup>(R.id.messageFileImageView)
        val fileDownloadProgress by bind<ProgressBar>(R.id.messageFileProgressbar)
        val filenameView by bind<TextView>(R.id.messageFilenameView)
    }

    companion object {
        private val STUB_ID = R.id.messageContentFileStub
    }
}
