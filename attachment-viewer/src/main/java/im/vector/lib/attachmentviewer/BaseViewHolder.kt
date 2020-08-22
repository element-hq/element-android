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
