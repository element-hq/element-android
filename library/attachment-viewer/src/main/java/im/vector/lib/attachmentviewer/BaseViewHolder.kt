/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.attachmentviewer

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

    open fun onRecycled() {
        boundResourceUid = null
    }

    open fun onAttached() {}
    open fun onDetached() {}
    open fun entersBackground() {}
    open fun entersForeground() {}
    open fun onSelected(selected: Boolean) {}

    open fun handleCommand(commands: AttachmentCommands) {}

    var boundResourceUid: String? = null

    open fun bind(attachmentInfo: AttachmentInfo) {
        boundResourceUid = attachmentInfo.uid
    }
}

class UnsupportedViewHolder constructor(itemView: View) :
        BaseViewHolder(itemView)
