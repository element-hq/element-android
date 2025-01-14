/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.lib.strings.CommonStrings
import me.gujun.android.span.image
import me.gujun.android.span.span
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass
abstract class SpaceChildInfoItem : VectorEpoxyModel<SpaceChildInfoItem.Holder>(R.layout.item_explore_space_child) {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var matrixItem: MatrixItem

    // Used only for diff calculation
    @EpoxyAttribute var topic: String? = null

    @EpoxyAttribute var memberCount: Int = 0
    @EpoxyAttribute var loading: Boolean = false
    @EpoxyAttribute var suggested: Boolean = false

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
        holder.titleView.text = matrixItem.displayName ?: holder.rootView.context.getString(CommonStrings.unnamed_room)
        avatarRenderer.render(matrixItem, holder.avatarImageView)

        holder.descriptionText.text = span {
            span {
                apply {
                    val tintColor = ThemeUtils.getColor(holder.view.context, im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
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

        holder.suggestedTag.visibility = if (suggested) View.VISIBLE else View.GONE
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
        val descriptionText by bind<TextView>(R.id.roomDescription)
        val suggestedTag by bind<TextView>(R.id.suggestedTag)
        val avatarImageView by bind<ImageView>(R.id.roomAvatarImageView)
        val rootView by bind<ViewGroup>(R.id.itemRoomLayout)
        val errorTextView by bind<TextView>(R.id.inlineErrorText)
    }
}
