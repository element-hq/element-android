/*
 * Copyright (c) 2021 New Vector Ltd
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
package fr.gouv.tchap.features.home.roomdirectory.createroom

import android.net.Uri
import android.text.Editable
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import fr.gouv.tchap.core.ui.views.HexagonMaskView
import fr.gouv.tchap.core.utils.TchapRoomType
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.epoxy.setValueOnce
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.platform.SimpleTextWatcher
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass(layout = R.layout.item_tchap_editable_avatar_with_name)
abstract class TchapRoomAvatarWithNameItem : EpoxyModelWithHolder<TchapRoomAvatarWithNameItem.Holder>() {

    @EpoxyAttribute
    var enabled: Boolean = true

    // Avatar
    @EpoxyAttribute
    var avatarRenderer: AvatarRenderer? = null

    @EpoxyAttribute
    var matrixItem: MatrixItem? = null

    @EpoxyAttribute
    var imageUri: Uri? = null

    @EpoxyAttribute
    var clickListener: ClickListener? = null

    @EpoxyAttribute
    var deleteListener: ClickListener? = null

    @EpoxyAttribute
    lateinit var roomType: TchapRoomType

    // Room name
    @EpoxyAttribute
    var hint: String? = null

    @EpoxyAttribute
    var value: String? = null

    @EpoxyAttribute
    var errorMessage: String? = null

    @EpoxyAttribute
    var inputType: Int? = null

    @EpoxyAttribute
    var singleLine: Boolean = true

    @EpoxyAttribute
    var imeOptions: Int? = null

    @EpoxyAttribute
    var endIconMode: Int? = null

    // FIXME restore EpoxyAttribute.Option.DoNotHash and fix that properly
    @EpoxyAttribute
    var onTextChange: ((String) -> Unit)? = null

    @EpoxyAttribute
    var editorActionListener: TextView.OnEditorActionListener? = null

    private val onTextChangeListener = object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            onTextChange?.invoke(s.toString())
        }
    }

    override fun bind(holder: Holder) {
        super.bind(holder)

        bindAvatar(holder)
        bindRoomName(holder)
    }

    private fun bindAvatar(holder: Holder) {
        holder.imageContainer.onClick(clickListener?.takeIf { enabled })
        if (matrixItem != null) {
            avatarRenderer?.render(matrixItem!!, holder.image)
        } else {
            GlideApp.with(holder.image)
                    .load(imageUri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(holder.image)
        }
        val isAvatarLoaded = imageUri != null || matrixItem?.avatarUrl?.isNotEmpty() == true
        holder.addImage.isVisible = enabled && !isAvatarLoaded
        // holder.delete.isVisible = enabled && isAvatarLoaded
        // holder.delete.onClick(deleteListener?.takeIf { enabled })
        holder.roomTypeIcon.setImageResource(when (roomType) {
            TchapRoomType.PRIVATE  -> R.drawable.ic_tchap_room_lock_red_bordered
            TchapRoomType.EXTERNAL -> R.drawable.ic_tchap_room_lock_orange_bordered
            TchapRoomType.FORUM    -> R.drawable.ic_tchap_forum
            else                   -> 0
        })
    }

    private fun bindRoomName(holder: Holder) {
        holder.textInputLayout.isEnabled = enabled
        holder.textInputLayout.hint = hint
        holder.textInputLayout.error = errorMessage
        holder.textInputLayout.endIconMode = endIconMode ?: TextInputLayout.END_ICON_NONE

        holder.setValueOnce(holder.textInputEditText, value)

        holder.textInputEditText.isEnabled = enabled
        inputType?.let { holder.textInputEditText.inputType = it }
        holder.textInputEditText.isSingleLine = singleLine
        holder.textInputEditText.imeOptions = imeOptions ?: EditorInfo.IME_ACTION_NONE

        holder.textInputEditText.addTextChangedListener(onTextChangeListener)
        holder.textInputEditText.setOnEditorActionListener(editorActionListener)
    }

    override fun shouldSaveViewState(): Boolean {
        return false
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        holder.textInputEditText.removeTextChangedListener(onTextChangeListener)
    }

    class Holder : VectorEpoxyHolder() {
        // Avatar
        val imageContainer by bind<View>(R.id.itemEditableAvatarImageContainer)
        val image by bind<HexagonMaskView>(R.id.itemEditableAvatarImage)
        val addImage by bind<View>(R.id.addImageIcon)
        val delete by bind<View>(R.id.itemEditableAvatarDelete)
        val roomTypeIcon by bind<ImageView>(R.id.itemEditableAvatarRoomType)

        // Room name
        val textInputLayout by bind<TextInputLayout>(R.id.formTextInputTextInputLayout)
        val textInputEditText by bind<TextInputEditText>(R.id.formTextInputTextInputEditText)
    }
}
