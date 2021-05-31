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

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.files.LocalFilesHelper
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.platform.TimelineMediaStateView
import im.vector.app.core.utils.setDebouncedClickListener
import im.vector.app.features.home.room.detail.timeline.helper.ContentDownloadStateTrackerBinder
import im.vector.app.features.home.room.detail.timeline.helper.ContentUploadStateTrackerBinder
import im.vector.app.features.media.ImageContentRenderer

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageImageVideoItem : AbsMessageItem<MessageImageVideoItem.Holder>() {

    @EpoxyAttribute
    lateinit var mediaData: ImageContentRenderer.Data

    @EpoxyAttribute
    var playable: Boolean = false

    @EpoxyAttribute
    var mode = ImageContentRenderer.Mode.THUMBNAIL

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var clickListener: View.OnClickListener? = null

    @EpoxyAttribute
    lateinit var imageContentRenderer: ImageContentRenderer

    @EpoxyAttribute
    lateinit var contentUploadStateTrackerBinder: ContentUploadStateTrackerBinder

    @EpoxyAttribute
    lateinit var contentDownloadStateTrackerBinder: ContentDownloadStateTrackerBinder

    @EpoxyAttribute
    var izLocalFile = false

    @EpoxyAttribute
    var izDownloaded = false

    @EpoxyAttribute
    var autoPlayGifs = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        if (!attributes.informationData.sendState.hasFailed()) {
            contentUploadStateTrackerBinder.bind(
                    attributes.informationData.eventId,
                    LocalFilesHelper(holder.view.context).isLocalFile(mediaData.url),
                    holder.progressLayout,
                    holder.mediaStateView
            )
        } else {
            holder.progressLayout.isVisible = false
        }

        if (!izDownloaded && !izLocalFile) {
            contentDownloadStateTrackerBinder.bind(
                    mediaData.url ?: "",
                    playable && !autoPlayGifs,
                    holder)
        } else {
            holder.mediaStateView.render(if (playable && !autoPlayGifs) TimelineMediaStateView.State.ReadyToPlay else TimelineMediaStateView.State.None)
        }
        holder.mediaStateView.setTag(R.id.messageMediaStateView, mediaData.url)
        imageContentRenderer.render(mediaData, mode, holder.imageView, autoPlayGifs, rendererCallbacks =  object: ImageContentRenderer.ContentRendererCallbacks {
            override fun onThumbnailModeFinish(success: Boolean) {
                // if a server thumbnail was used the download tracker won't be called
                mediaData.url?.let { mxcUrl ->
                    if (mxcUrl == holder.mediaStateView.getTag(R.id.messageMediaStateView)) {
                        contentDownloadStateTrackerBinder.unbind(mxcUrl)
                        // mmm annoying but have to post if not the previous contentDownloadStateTrackerBinder.bind call we render
                        // an IDLE state (i.e a loading wheel...)
                        holder.mediaStateView.post {
                            holder.mediaStateView.render(if (success) TimelineMediaStateView.State.None else TimelineMediaStateView.State.PermanentError)
                        }
                    }
                }
            }

            override fun onLoadModeFinish(success: Boolean) {
                // if a server thumbnail was used the download tracker won't be called
                mediaData.url?.let { mxcUrl ->
                    if (mxcUrl == holder.mediaStateView.getTag(R.id.messageMediaStateView)) {
                        contentDownloadStateTrackerBinder.unbind(mxcUrl)
                        // mmm annoying but have to post if not the previous contentDownloadStateTrackerBinder.bind call we render
                        // an IDLE state (i.e a loading wheel...)
                        holder.mediaStateView.post {
                            holder.mediaStateView.render(if (success) TimelineMediaStateView.State.None else TimelineMediaStateView.State.PermanentError)
                        }
                    }
                }
            }
        })

        holder.mediaStateView.callback = object : TimelineMediaStateView.Callback {
            override fun onButtonClicked() {
                // for now delegate to regular click
                clickListener?.onClick(holder.imageView)
            }
        }
        holder.imageView.setDebouncedClickListener(clickListener)
        holder.imageView.setOnLongClickListener(attributes.itemLongClickListener)
        ViewCompat.setTransitionName(holder.imageView, "imagePreview_${id()}")
        holder.mediaContentView.setDebouncedClickListener(attributes.itemClickListener)
        holder.mediaContentView.setOnLongClickListener(attributes.itemLongClickListener)
        // holder.playContentView.visibility = if (playable) View.VISIBLE else View.GONE
    }

    override fun unbind(holder: Holder) {
        holder.mediaStateView.setTag(R.id.messageMediaStateView, null)
        GlideApp.with(holder.view.context.applicationContext).clear(holder.imageView)
        imageContentRenderer.clear(holder.imageView)
        contentUploadStateTrackerBinder.unbind(attributes.informationData.eventId)
        contentDownloadStateTrackerBinder.unbind(mediaData.url ?: "")
        holder.imageView.setOnClickListener(null)
        holder.imageView.setOnLongClickListener(null)
        super.unbind(holder)
    }

    override fun getViewType() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val progressLayout by bind<ViewGroup>(R.id.messageMediaUploadProgressLayout)
        val mediaStateView by bind<TimelineMediaStateView>(R.id.messageMediaStateView)
        val imageView by bind<ImageView>(R.id.messageThumbnailView)

        //        val playContentView by bind<ImageView>(R.id.messageMediaPlayView)
        val mediaContentView by bind<ViewGroup>(R.id.messageContentMedia)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentMediaStub
    }
}
