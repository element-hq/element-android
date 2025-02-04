/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.attachmentviewer

import android.view.View
import im.vector.lib.attachmentviewer.databinding.ItemAnimatedImageAttachmentBinding

class AnimatedImageViewHolder constructor(itemView: View) :
        BaseViewHolder(itemView) {

    val views = ItemAnimatedImageAttachmentBinding.bind(itemView)

    internal val target = DefaultImageLoaderTarget(this, views.imageView)

    override fun onRecycled() {
        super.onRecycled()
        views.imageView.setImageDrawable(null)
    }
}
