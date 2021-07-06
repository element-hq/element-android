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

package im.vector.app.features.userdirectory

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.amulyakhare.textdrawable.TextDrawable
import fr.gouv.tchap.core.utils.TchapUtils
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass(layout = R.layout.item_tchap_known_user)
abstract class UserDirectoryUserItem : VectorEpoxyModel<UserDirectoryUserItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute var clickListener: View.OnClickListener? = null
    @EpoxyAttribute var selected: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.view.setOnClickListener(clickListener)
        val displayName = matrixItem.displayName
                ?.takeUnless { it.isEmpty() }
                ?: TchapUtils.computeDisplayNameFromUserId(matrixItem.id).orEmpty()
        if (TchapUtils.isExternalTchapUser(matrixItem.id)) {
            holder.nameView.text = displayName
            holder.domainView.text = holder.view.context.resources.getString(R.string.tchap_contact_external)
            holder.domainView.setTextColor(ContextCompat.getColor(holder.view.context, R.color.tchap_contact_external_color))
        } else {
            holder.nameView.text = TchapUtils.getNameFromDisplayName(displayName)
            holder.domainView.text = TchapUtils.getDomainFromDisplayName(displayName)
            holder.domainView.setTextColor(ThemeUtils.getColor(holder.view.context, R.attr.secondary_text_color))
        }
        holder.statusImageView.isVisible = false // Todo: Handle user status
        renderSelection(holder, selected)
    }

    private fun renderSelection(holder: Holder, isSelected: Boolean) {
        if (isSelected) {
            holder.avatarCheckedImageView.visibility = View.VISIBLE
            val backgroundColor = ContextCompat.getColor(holder.view.context, R.color.riotx_accent)
            val backgroundDrawable = TextDrawable.builder().buildRound("", backgroundColor)
            holder.avatarImageView.setImageDrawable(backgroundDrawable)
        } else {
            holder.avatarCheckedImageView.visibility = View.GONE
            avatarRenderer.render(matrixItem, holder.avatarImageView)
        }
    }

    class Holder : VectorEpoxyHolder() {
        val nameView by bind<TextView>(R.id.knownUserName)
        val domainView by bind<TextView>(R.id.knownUserDomain)
        val avatarImageView by bind<ImageView>(R.id.knownUserAvatar)
        val avatarCheckedImageView by bind<ImageView>(R.id.knownUserAvatarChecked)
        val statusImageView by bind<ImageView>(R.id.knownUserStatus)
    }
}
