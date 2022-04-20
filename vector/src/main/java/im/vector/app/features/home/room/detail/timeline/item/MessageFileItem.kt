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

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageFileItem : AbsMessageItem<MessageFileItem.Holder>() {

    @EpoxyAttribute
    var filename: String = ""

    @EpoxyAttribute
    var mxcUrl: String = ""

    @EpoxyAttribute
    @DrawableRes
    var iconRes: Int = 0

    @EpoxyAttribute
    @JvmField
    var isLocalFile = false

    @EpoxyAttribute
    @JvmField
    var isDownloaded = false

    @EpoxyAttribute
    lateinit var contentUploadStateTrackerBinder: ContentUploadStateTrackerBinder

    @EpoxyAttribute
    lateinit var contentDownloadStateTrackerBinder: ContentDownloadStateTrackerBinder

    override fun bind(holder: Holder) {
        super.bind(holder)
        renderSendState(holder.fileLayout, holder.filenameView)

        if (!attributes.informationData.sendState.hasFailed()) {
            contentUploadStateTrackerBinder.bind(attributes.informationData.eventId, isLocalFile, holder.progressLayout)
        } else {
            holder.fileImageView.setImageResource(R.drawable.ic_cross)
            holder.progressLayout.isVisible = false
        }

        holder.filenameView.text = filename

        if (attributes.informationData.sendState.isSending()) {
            holder.fileImageView.setImageResource(iconRes)
        } else {
            if (isDownloaded) {
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
            ThemeUtils.getColor(holder.view.context, R.attr.vctr_content_quinary)
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
        private const val STUB_ID = R.id.messageContentFileStub
    }
}
