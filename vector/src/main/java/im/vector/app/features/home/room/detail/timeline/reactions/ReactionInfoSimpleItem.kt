/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.reactions

import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence

/**
 * Item displaying an emoji reaction (single line with emoji, author, time).
 */
@EpoxyModelClass
abstract class ReactionInfoSimpleItem : VectorEpoxyModel<ReactionInfoSimpleItem.Holder>(R.layout.item_simple_reaction_info) {

    @EpoxyAttribute
    lateinit var reactionKey: EpoxyCharSequence

    @EpoxyAttribute
    lateinit var authorDisplayName: String

    @EpoxyAttribute
    var timeStamp: String? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var userClicked: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.emojiReactionView.text = reactionKey.charSequence
        holder.displayNameView.text = authorDisplayName
        timeStamp?.let {
            holder.timeStampView.text = it
            holder.timeStampView.isVisible = true
        } ?: run {
            holder.timeStampView.isVisible = false
        }
        holder.view.onClick(userClicked)
    }

    class Holder : VectorEpoxyHolder() {
        val emojiReactionView by bind<TextView>(R.id.itemSimpleReactionInfoKey)
        val displayNameView by bind<TextView>(R.id.itemSimpleReactionInfoMemberName)
        val timeStampView by bind<TextView>(R.id.itemSimpleReactionInfoTime)
    }
}
