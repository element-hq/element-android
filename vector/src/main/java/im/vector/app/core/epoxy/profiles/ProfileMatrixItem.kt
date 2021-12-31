/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.core.epoxy.profiles

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.ui.views.PresenceStateImageView
import im.vector.app.core.ui.views.ShieldImageView

@EpoxyModelClass(layout = R.layout.item_profile_matrix_item)
abstract class ProfileMatrixItem : BaseProfileMatrixItem<ProfileMatrixItem.Holder>() {

    open class Holder : VectorEpoxyHolder() {
        val titleView by bind<TextView>(R.id.matrixItemTitle)
        val subtitleView by bind<TextView>(R.id.matrixItemSubtitle)
        val powerLabel by bind<TextView>(R.id.matrixItemPowerLevelLabel)
        val presenceImageView by bind<PresenceStateImageView>(R.id.matrixItemPresenceImageView)
        val avatarImageView by bind<ImageView>(R.id.matrixItemAvatar)
        val avatarDecorationImageView by bind<ShieldImageView>(R.id.matrixItemAvatarDecoration)
        val editableView by bind<View>(R.id.matrixItemEditable)
    }
}
