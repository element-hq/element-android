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

package im.vector.riotx.attachmentviewer

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition

class AnimatedImageViewHolder constructor(itemView: View) :
        BaseViewHolder(itemView) {

    val touchImageView: ImageView = itemView.findViewById(R.id.imageView)
    val imageLoaderProgress: ProgressBar = itemView.findViewById(R.id.imageLoaderProgress)

    val customTargetView = object : CustomViewTarget<ImageView, Drawable>(touchImageView) {

        override fun onResourceLoading(placeholder: Drawable?) {
            imageLoaderProgress.isVisible = true
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
            imageLoaderProgress.isVisible = false
        }

        override fun onResourceCleared(placeholder: Drawable?) {
            touchImageView.setImageDrawable(placeholder)
        }

        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
            imageLoaderProgress.isVisible = false
            // Glide mess up the view size :/
            touchImageView.updateLayoutParams {
                width = LinearLayout.LayoutParams.MATCH_PARENT
                height = LinearLayout.LayoutParams.MATCH_PARENT
            }
            touchImageView.setImageDrawable(resource)
            if (resource is Animatable) {
                resource.start()
            }
        }
    }

    override fun bind(attachmentInfo: AttachmentInfo) {
    }
}
