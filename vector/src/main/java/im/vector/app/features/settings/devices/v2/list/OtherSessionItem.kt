/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.list

import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.TextView
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
        holder.otherSessionDescriptionTextView.setCompoundDrawablesWithIntrinsicBounds(sessionDescriptionDrawable, null, null, null)
    }

    class Holder : VectorEpoxyHolder() {
        val otherSessionDeviceTypeImageView by bind<ImageView>(R.id.otherSessionDeviceTypeImageView)
        val otherSessionVerificationStatusImageView by bind<ShieldImageView>(R.id.otherSessionVerificationStatusImageView)
        val otherSessionNameTextView by bind<TextView>(R.id.otherSessionNameTextView)
        val otherSessionDescriptionTextView by bind<TextView>(R.id.otherSessionDescriptionTextView)
    }
}
