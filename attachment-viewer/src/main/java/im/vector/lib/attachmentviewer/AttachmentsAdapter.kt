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

package im.vector.lib.attachmentviewer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class AttachmentsAdapter : RecyclerView.Adapter<BaseViewHolder>() {

    var attachmentSourceProvider: AttachmentSourceProvider? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.item_image_attachment          -> ZoomableImageViewHolder(itemView)
            R.layout.item_animated_image_attachment -> AnimatedImageViewHolder(itemView)
            R.layout.item_video_attachment          -> VideoViewHolder(itemView)
            else                                    -> UnsupportedViewHolder(itemView)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val info = attachmentSourceProvider!!.getAttachmentInfoAt(position)
        return when (info) {
            is AttachmentInfo.Image         -> R.layout.item_image_attachment
            is AttachmentInfo.Video         -> R.layout.item_video_attachment
            is AttachmentInfo.AnimatedImage -> R.layout.item_animated_image_attachment
//            is AttachmentInfo.Audio         -> TODO()
//            is AttachmentInfo.File          -> TODO()
        }
    }

    override fun getItemCount(): Int {
        return attachmentSourceProvider?.getItemCount() ?: 0
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        attachmentSourceProvider?.getAttachmentInfoAt(position)?.let {
            holder.bind(it)
            when (it) {
                is AttachmentInfo.Image         -> {
                    attachmentSourceProvider?.loadImage((holder as ZoomableImageViewHolder).target, it)
                }
                is AttachmentInfo.AnimatedImage -> {
                    attachmentSourceProvider?.loadImage((holder as AnimatedImageViewHolder).target, it)
                }
                is AttachmentInfo.Video         -> {
                    attachmentSourceProvider?.loadVideo((holder as VideoViewHolder).target, it)
                }
//                else                            -> {
// //                }
            }
        }
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        holder.onAttached()
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        holder.onRecycled()
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder) {
        holder.onDetached()
    }

    fun isScaled(position: Int): Boolean {
        val holder = recyclerView?.findViewHolderForAdapterPosition(position)
        if (holder is ZoomableImageViewHolder) {
            return holder.views.touchImageView.attacher.scale > 1f
        }
        return false
    }

    fun onPause(position: Int) {
        val holder = recyclerView?.findViewHolderForAdapterPosition(position) as? BaseViewHolder
        holder?.entersBackground()
    }

    fun onResume(position: Int) {
        val holder = recyclerView?.findViewHolderForAdapterPosition(position) as? BaseViewHolder
        holder?.entersForeground()
    }
}
