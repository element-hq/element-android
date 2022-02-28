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

import android.view.View
import im.vector.lib.attachmentviewer.databinding.ItemImageAttachmentBinding

class ZoomableImageViewHolder constructor(itemView: View) :
        BaseViewHolder(itemView) {

    val views = ItemImageAttachmentBinding.bind(itemView)

    init {
        views.touchImageView.setAllowParentInterceptOnEdge(false)
        views.touchImageView.setOnScaleChangeListener { scaleFactor, _, _ ->
            // Log.v("ATTACHEMENTS", "scaleFactor $scaleFactor")
            // It's a bit annoying but when you pitch down the scaling
            // is not exactly one :/
            views.touchImageView.setAllowParentInterceptOnEdge(scaleFactor <= 1.0008f)
        }
        views.touchImageView.setScale(1.0f, true)
        views.touchImageView.setAllowParentInterceptOnEdge(true)
    }

    internal val target = DefaultImageLoaderTarget.ZoomableImageTarget(this, views.touchImageView)

    override fun onRecycled() {
        super.onRecycled()
        views.touchImageView.setImageDrawable(null)
    }
}
