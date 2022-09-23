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

import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.views.ShieldImageView
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

@EpoxyModelClass
abstract class OtherSessionItem : VectorEpoxyModel<OtherSessionItem.Holder>(R.layout.item_other_session) {

    @EpoxyAttribute
    var deviceType: DeviceType = DeviceType.UNKNOWN

    @EpoxyAttribute
    var roomEncryptionTrustLevel: RoomEncryptionTrustLevel? = null

    @EpoxyAttribute
    var sessionName: String? = null

    @EpoxyAttribute
    var sessionDescription: String? = null

    @EpoxyAttribute
    @ColorInt
    var sessionDescriptionColor: Int? = null

    @EpoxyAttribute
    var sessionDescriptionDrawable: Drawable? = null

    @EpoxyAttribute
    lateinit var stringProvider: StringProvider

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var clickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.view.onClick(clickListener)
        if (clickListener == null) {
            holder.view.isClickable = false
        }

        when (deviceType) {
            DeviceType.MOBILE -> {
                holder.otherSessionDeviceTypeImageView.setImageResource(R.drawable.ic_device_type_mobile)
                holder.otherSessionDeviceTypeImageView.contentDescription = stringProvider.getString(R.string.a11y_device_manager_device_type_mobile)
            }
            DeviceType.WEB -> {
                holder.otherSessionDeviceTypeImageView.setImageResource(R.drawable.ic_device_type_web)
                holder.otherSessionDeviceTypeImageView.contentDescription = stringProvider.getString(R.string.a11y_device_manager_device_type_web)
            }
            DeviceType.DESKTOP -> {
                holder.otherSessionDeviceTypeImageView.setImageResource(R.drawable.ic_device_type_desktop)
                holder.otherSessionDeviceTypeImageView.contentDescription = stringProvider.getString(R.string.a11y_device_manager_device_type_desktop)
            }
            DeviceType.UNKNOWN -> {
                holder.otherSessionDeviceTypeImageView.setImageResource(R.drawable.ic_device_type_unknown)
                holder.otherSessionDeviceTypeImageView.contentDescription = stringProvider.getString(R.string.a11y_device_manager_device_type_unknown)
            }
        }
        holder.otherSessionVerificationStatusImageView.render(roomEncryptionTrustLevel)
        holder.otherSessionNameTextView.text = sessionName
        holder.otherSessionDescriptionTextView.text = sessionDescription
        sessionDescriptionColor?.let {
            holder.otherSessionDescriptionTextView.setTextColor(it)
        }
        holder.otherSessionDescriptionTextView.setCompoundDrawablesWithIntrinsicBounds(sessionDescriptionDrawable, null, null, null)
    }

    class Holder : VectorEpoxyHolder() {
        val otherSessionDeviceTypeImageView by bind<ImageView>(R.id.otherSessionDeviceTypeImageView)
        val otherSessionVerificationStatusImageView by bind<ShieldImageView>(R.id.otherSessionVerificationStatusImageView)
        val otherSessionNameTextView by bind<TextView>(R.id.otherSessionNameTextView)
        val otherSessionDescriptionTextView by bind<TextView>(R.id.otherSessionDescriptionTextView)
    }
}
