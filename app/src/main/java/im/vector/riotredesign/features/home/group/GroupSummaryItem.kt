/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotredesign.features.home.group

import android.widget.ImageView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel
import im.vector.riotredesign.core.platform.CheckableFrameLayout
import im.vector.riotredesign.features.home.AvatarRenderer


data class GroupSummaryItem(
        val groupName: CharSequence,
        val avatarUrl: String?,
        val isSelected: Boolean,
        val listener: (() -> Unit)? = null
) : KotlinModel(R.layout.item_group) {

    private val avatarImageView by bind<ImageView>(R.id.groupAvatarImageView)
    private val rootView by bind<CheckableFrameLayout>(R.id.itemGroupLayout)

    override fun bind() {
        rootView.isSelected = isSelected
        rootView.setOnClickListener { listener?.invoke() }
        AvatarRenderer.render(avatarUrl, groupName.toString(), avatarImageView)
    }
}