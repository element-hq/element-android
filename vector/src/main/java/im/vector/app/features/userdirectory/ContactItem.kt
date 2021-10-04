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

import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.contacts.MappedContact
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.features.home.AvatarRenderer

@EpoxyModelClass(layout = R.layout.item_contact_main)
abstract class ContactItem : VectorEpoxyModel<ContactItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var mappedContact: MappedContact

    override fun bind(holder: Holder) {
        super.bind(holder)
        // If name is empty, use userId as name and force it being centered
        holder.nameView.text = mappedContact.displayName
        avatarRenderer.render(mappedContact, holder.avatarImageView)
    }

    class Holder : VectorEpoxyHolder() {
        val nameView by bind<TextView>(R.id.contactDisplayName)
        val avatarImageView by bind<ImageView>(R.id.contactAvatar)
    }
}
