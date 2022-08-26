/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.settings.devices.v2.list

import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.ui.views.ShieldImageView
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

@EpoxyModelClass
abstract class OtherSessionItem : VectorEpoxyModel<OtherSessionItem.Holder>(R.layout.item_other_session) {

    @EpoxyAttribute
    var deviceType: SessionDeviceType = SessionDeviceType.UNKNOWN

    @EpoxyAttribute
    var roomEncryptionTrustLevel: RoomEncryptionTrustLevel? = null

    @EpoxyAttribute
    var sessionName: String? = null

    @EpoxyAttribute
    var sessionDescription: String? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.otherSessionDeviceTypeImageView.setImageResource(
                when (deviceType) {
                    SessionDeviceType.MOBILE -> R.drawable.ic_device_type_mobile
                    SessionDeviceType.WEB -> R.drawable.ic_device_type_web
                    SessionDeviceType.DESKTOP -> R.drawable.ic_device_type_desktop
                    SessionDeviceType.UNKNOWN -> R.drawable.ic_device_type_unknown
                }
        )
        holder.otherSessionVerificationStatusImageView.render(roomEncryptionTrustLevel)
        holder.otherSessionNameTextView.text = sessionName
        holder.otherSessionDescriptionTextView.text = sessionDescription
    }

    class Holder : VectorEpoxyHolder() {
        val otherSessionDeviceTypeImageView by bind<ImageView>(R.id.otherSessionDeviceTypeImageView)
        val otherSessionVerificationStatusImageView by bind<ShieldImageView>(R.id.otherSessionVerificationStatusImageView)
        val otherSessionNameTextView by bind<TextView>(R.id.otherSessionNameTextView)
        val otherSessionDescriptionTextView by bind<TextView>(R.id.otherSessionDescriptionTextView)
    }
}
