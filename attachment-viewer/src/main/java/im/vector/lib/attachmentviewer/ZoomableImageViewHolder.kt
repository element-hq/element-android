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
import android.widget.ProgressBar
import com.github.chrisbanes.photoview.PhotoView

class ZoomableImageViewHolder constructor(itemView: View) :
        BaseViewHolder(itemView) {

    val touchImageView: PhotoView = itemView.findViewById(R.id.touchImageView)
    val imageLoaderProgress: ProgressBar = itemView.findViewById(R.id.imageLoaderProgress)

    init {
        touchImageView.setAllowParentInterceptOnEdge(false)
        touchImageView.setOnScaleChangeListener { scaleFactor, _, _ ->
            // Log.v("ATTACHEMENTS", "scaleFactor $scaleFactor")
            // It's a bit annoying but when you pitch down the scaling
            // is not exactly one :/
            touchImageView.setAllowParentInterceptOnEdge(scaleFactor <= 1.0008f)
        }
        touchImageView.setScale(1.0f, true)
        touchImageView.setAllowParentInterceptOnEdge(true)
    }

    internal val target = DefaultImageLoaderTarget.ZoomableImageTarget(this, touchImageView)
}
