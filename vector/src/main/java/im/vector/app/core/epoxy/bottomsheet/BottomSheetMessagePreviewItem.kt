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
 *
 */
package im.vector.app.core.epoxy.bottomsheet

import android.text.method.MovementMethod
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.tools.findPillsAndProcess
import im.vector.app.features.media.ImageContentRenderer
import org.matrix.android.sdk.api.util.MatrixItem

/**
 * A message preview for bottom sheet.
 */
@EpoxyModelClass(layout = R.layout.item_bottom_sheet_message_preview)
abstract class BottomSheetMessagePreviewItem : VectorEpoxyModel<BottomSheetMessagePreviewItem.Holder>() {

    @EpoxyAttribute
    lateinit var avatarRenderer: AvatarRenderer

    @EpoxyAttribute
    lateinit var matrixItem: MatrixItem

    @EpoxyAttribute
    lateinit var body: CharSequence

    @EpoxyAttribute
    var bodyDetails: CharSequence? = null

    @EpoxyAttribute
    var imageContentRenderer: ImageContentRenderer? = null

    @EpoxyAttribute
    var data: ImageContentRenderer.Data? = null

    @EpoxyAttribute
    var time: CharSequence? = null

    @EpoxyAttribute
    var movementMethod: MovementMethod? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var userClicked: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        avatarRenderer.render(matrixItem, holder.avatar)
        holder.avatar.onClick(userClicked)
        holder.sender.onClick(userClicked)
        holder.sender.setTextOrHide(matrixItem.displayName)
        data?.let {
            imageContentRenderer?.render(it, ImageContentRenderer.Mode.THUMBNAIL, holder.imagePreview)
        }
        holder.imagePreview.isVisible = data != null
        holder.body.movementMethod = movementMethod
        holder.body.text = body
        holder.bodyDetails.setTextOrHide(bodyDetails)
        body.findPillsAndProcess(coroutineScope) { it.bind(holder.body) }
        holder.timestamp.setTextOrHide(time)
    }

    override fun unbind(holder: Holder) {
        imageContentRenderer?.clear(holder.imagePreview)
        super.unbind(holder)
    }

    class Holder : VectorEpoxyHolder() {
        val avatar by bind<ImageView>(R.id.bottom_sheet_message_preview_avatar)
        val sender by bind<TextView>(R.id.bottom_sheet_message_preview_sender)
        val body by bind<TextView>(R.id.bottom_sheet_message_preview_body)
        val bodyDetails by bind<TextView>(R.id.bottom_sheet_message_preview_body_details)
        val timestamp by bind<TextView>(R.id.bottom_sheet_message_preview_timestamp)
        val imagePreview by bind<ImageView>(R.id.bottom_sheet_message_preview_image)
    }
}
