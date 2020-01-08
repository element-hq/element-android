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

package im.vector.riotx.features.settings.devices

import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.matrix.android.internal.crypto.model.rest.DeviceInfo
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * A list item for Device.
 */
@EpoxyModelClass(layout = R.layout.item_device)
abstract class DeviceItem : VectorEpoxyModel<DeviceItem.Holder>() {

    @EpoxyAttribute
    lateinit var deviceInfo: DeviceInfo

    @EpoxyAttribute
    var currentDevice = false

    @EpoxyAttribute
    var buttonsVisible = false

    @EpoxyAttribute
    var itemClickAction: (() -> Unit)? = null

    @EpoxyAttribute
    var renameClickAction: (() -> Unit)? = null

    @EpoxyAttribute
    var deleteClickAction: (() -> Unit)? = null

    override fun bind(holder: Holder) {
        holder.root.setOnClickListener { itemClickAction?.invoke() }

        holder.displayNameText.text = deviceInfo.displayName ?: ""
        holder.deviceIdText.text = deviceInfo.deviceId ?: ""

        val lastSeenIp = deviceInfo.lastSeenIp?.takeIf { ip -> ip.isNotBlank() } ?: "-"

        val lastSeenTime = deviceInfo.lastSeenTs?.let { ts ->
            val dateFormatTime = SimpleDateFormat("HH:mm:ss", Locale.ROOT)
            val date = Date(ts)

            val time = dateFormatTime.format(date)
            val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())

            dateFormat.format(date) + ", " + time
        } ?: "-"

        holder.deviceLastSeenText.text = holder.root.context.getString(R.string.devices_details_last_seen_format, lastSeenIp, lastSeenTime)

        listOf(
                holder.displayNameLabelText,
                holder.displayNameText,
                holder.deviceIdLabelText,
                holder.deviceIdText,
                holder.deviceLastSeenLabelText,
                holder.deviceLastSeenText
        ).map {
            it.setTypeface(null, if (currentDevice) Typeface.BOLD else Typeface.NORMAL)
        }

        holder.buttonDelete.isVisible = !currentDevice

        holder.buttons.isVisible = buttonsVisible

        holder.buttonRename.setOnClickListener { renameClickAction?.invoke() }
        holder.buttonDelete.setOnClickListener { deleteClickAction?.invoke() }
    }

    class Holder : VectorEpoxyHolder() {
        val root by bind<ViewGroup>(R.id.itemDeviceRoot)
        val displayNameLabelText by bind<TextView>(R.id.itemDeviceDisplayNameLabel)
        val displayNameText by bind<TextView>(R.id.itemDeviceDisplayName)
        val deviceIdLabelText by bind<TextView>(R.id.itemDeviceIdLabel)
        val deviceIdText by bind<TextView>(R.id.itemDeviceId)
        val deviceLastSeenLabelText by bind<TextView>(R.id.itemDeviceLastSeenLabel)
        val deviceLastSeenText by bind<TextView>(R.id.itemDeviceLastSeen)
        val buttons by bind<View>(R.id.itemDeviceButtons)
        val buttonDelete by bind<View>(R.id.itemDeviceDelete)
        val buttonRename by bind<View>(R.id.itemDeviceRename)
    }
}
