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

package im.vector.app.features.crypto.keysbackup.settings

import android.view.View
import android.widget.Button
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.extensions.setTextOrHide

@EpoxyModelClass(layout = R.layout.item_keys_backup_settings_button_footer)
abstract class KeysBackupSettingFooterItem : VectorEpoxyModel<KeysBackupSettingFooterItem.Holder>() {

    @EpoxyAttribute
    var textButton1: String? = null

    @EpoxyAttribute
    var clickOnButton1: View.OnClickListener? = null

    @EpoxyAttribute
    var textButton2: String? = null

    @EpoxyAttribute
    var clickOnButton2: View.OnClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.button1.setTextOrHide(textButton1)
        holder.button1.setOnClickListener(clickOnButton1)

        holder.button2.setTextOrHide(textButton2)
        holder.button2.setOnClickListener(clickOnButton2)
    }

    class Holder : VectorEpoxyHolder() {
        val button1 by bind<Button>(R.id.keys_backup_settings_footer_button1)
        val button2 by bind<TextView>(R.id.keys_backup_settings_footer_button2)
    }
}
