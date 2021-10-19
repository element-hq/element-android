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
 */

package im.vector.app.features.home.room.list

import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
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
import im.vector.app.features.themes.ThemeUtils
import me.gujun.android.span.image
import me.gujun.android.span.span
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass(layout = R.layout.item_suggested_room)
abstract class SpaceChildInfoItem : VectorEpoxyModel<SpaceChildInfoItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var matrixItem: MatrixItem

    // Used only for diff calculation
    @EpoxyAttribute var topic: String? = null

    @EpoxyAttribute var memberCount: Int = 0
    @EpoxyAttribute var loading: Boolean = false

    @EpoxyAttribute var buttonLabel: String? = null
    @EpoxyAttribute var errorLabel: String? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var itemLongClickListener: View.OnLongClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var itemClickListener: ClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var buttonClickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.rootView.onClick(itemClickListener)
        holder.rootView.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            itemLongClickListener?.onLongClick(it) ?: false
        }
        holder.titleView.text = matrixItem.displayName ?: holder.rootView.context.getString(R.string.unnamed_room)
        avatarRenderer.render(matrixItem, holder.avatarImageView)

        holder.descriptionText.text = span {
            span {
                apply {
                    val tintColor = ThemeUtils.getColor(holder.view.context, R.attr.vctr_content_secondary)
                    ContextCompat.getDrawable(holder.view.context, R.drawable.ic_member_small)
                            ?.apply {
                                ThemeUtils.tintDrawableWithColor(this, tintColor)
                            }?.let {
                                image(it)
                            }
                }
                +" $memberCount"
                apply {
                    topic?.let {
                        +" - $topic"
                    }
                }
            }
        }

        holder.joinButton.text = buttonLabel

        if (loading) {
            holder.joinButtonLoading.isVisible = true
            holder.joinButton.isInvisible = true
        } else {
            holder.joinButtonLoading.isVisible = false
            holder.joinButton.isVisible = true
        }

        holder.errorTextView.setTextOrHide(errorLabel)

        holder.joinButton.onClick {
            // local echo
            holder.joinButton.isEnabled = false
            // FIXME It may lead to crash if the view is gone
            holder.view.postDelayed({ holder.joinButton.isEnabled = true }, 400)
            buttonClickListener?.invoke(it)
        }
    }

    override fun unbind(holder: Holder) {
        holder.rootView.setOnClickListener(null)
        holder.rootView.setOnLongClickListener(null)
        avatarRenderer.clear(holder.avatarImageView)
        super.unbind(holder)
    }

    class Holder : VectorEpoxyHolder() {
        val titleView by bind<TextView>(R.id.roomNameView)
        val joinButton by bind<Button>(R.id.joinSuggestedRoomButton)
        val joinButtonLoading by bind<ProgressBar>(R.id.joinSuggestedLoading)
        val descriptionText by bind<TextView>(R.id.suggestedRoomDescription)
        val avatarImageView by bind<ImageView>(R.id.roomAvatarImageView)
        val rootView by bind<ViewGroup>(R.id.itemRoomLayout)
        val errorTextView by bind<TextView>(R.id.inlineErrorText)
    }
}
