/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.riotredesign.core.ui.list

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import im.vector.riotredesign.R

/**
 * View Holder for generic list items.
 * Displays an item with a title, and optional description.
 * Can display an accessory on the right, that can be an image or an indeterminate progress.
 * If provided with an action, will display a button at the bottom of the list item.
 */
class GenericItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        @LayoutRes
        const val resId = R.layout.item_generic_list
    }

    @BindView(R.id.item_generic_title_text)
    lateinit var titleText: TextView

    @BindView(R.id.item_generic_description_text)
    lateinit var descriptionText: TextView

    @BindView(R.id.item_generic_accessory_image)
    lateinit var accessoryImage: ImageView

    @BindView(R.id.item_generic_progress_bar)
    lateinit var progressBar: ProgressBar

    @BindView(R.id.item_generic_action_button)
    lateinit var actionButton: Button

    init {
        ButterKnife.bind(this, itemView)
    }

    fun bind(item: GenericRecyclerViewItem) {
        titleText.text = item.title

        when (item.style) {
            GenericRecyclerViewItem.STYLE.BIG_TEXT -> titleText.textSize = 18f
            GenericRecyclerViewItem.STYLE.NORMAL_TEXT -> titleText.textSize = 14f
        }

        item.description?.let {
            descriptionText.isVisible = true
            descriptionText.text = it
        } ?: run { descriptionText.isVisible = false }

        if (item.hasIndeterminateProcess) {
            progressBar.isVisible = true
            accessoryImage.isVisible = false
        } else {
            progressBar.isVisible = false
            if (item.endIconResourceId != -1) {
                accessoryImage.setImageResource(item.endIconResourceId)
                accessoryImage.isVisible = true
            } else {
                accessoryImage.isVisible = false
            }
        }

        val buttonAction = item.buttonAction

        if (buttonAction == null) {
            actionButton.isVisible = false
        } else {
            actionButton.text = buttonAction.title
            actionButton.setOnClickListener {
                buttonAction.perform?.run()
            }
            actionButton.isVisible = true
        }

        itemView?.setOnClickListener {
            item.itemClickAction?.perform?.run()
        }
    }
}