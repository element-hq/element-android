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

package im.vector.app.features.roomprofile.uploads.files

import android.view.View
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.extensions.setTextOrHide

@EpoxyModelClass(layout = R.layout.item_uploads_file)
abstract class UploadsFileItem : VectorEpoxyModel<UploadsFileItem.Holder>() {

    @EpoxyAttribute var title: String? = null
    @EpoxyAttribute var subtitle: String? = null

    @EpoxyAttribute var listener: Listener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.view.setOnClickListener { listener?.onItemClicked() }
        holder.titleView.text = title
        holder.subtitleView.setTextOrHide(subtitle)
        holder.downloadView.setOnClickListener { listener?.onDownloadClicked() }
        holder.shareView.setOnClickListener { listener?.onShareClicked() }
    }

    class Holder : VectorEpoxyHolder() {
        val titleView by bind<TextView>(R.id.uploadsFileTitle)
        val subtitleView by bind<TextView>(R.id.uploadsFileSubtitle)
        val downloadView by bind<View>(R.id.uploadsFileActionDownload)
        val shareView by bind<View>(R.id.uploadsFileActionShare)
    }

    interface Listener {
        fun onItemClicked()
        fun onDownloadClicked()
        fun onShareClicked()
    }
}
