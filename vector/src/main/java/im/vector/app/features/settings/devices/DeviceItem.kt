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

package im.vector.app.features.settings.devices

import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import org.matrix.android.sdk.internal.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.model.rest.DeviceInfo
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.DimensionConverter
import me.gujun.android.span.span
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var itemClickAction: (() -> Unit)? = null

    @EpoxyAttribute
    var detailedMode = false

    @EpoxyAttribute
    var trusted: DeviceTrustLevel? = null

    @EpoxyAttribute
    var e2eCapable: Boolean = true

    @EpoxyAttribute
    var legacyMode: Boolean = false

    @EpoxyAttribute
    var trustedSession: Boolean = false

    @EpoxyAttribute
    var colorProvider: ColorProvider? = null

    @EpoxyAttribute
    var dimensionConverter: DimensionConverter? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.root.setOnClickListener { itemClickAction?.invoke() }

        val shield = TrustUtils.shieldForTrust(
                currentDevice,
                trustedSession,
                legacyMode,
                trusted
        )

        if (e2eCapable) {
            holder.trustIcon.setImageResource(shield)
        } else {
            holder.trustIcon.setImageDrawable(null)
        }

        val detailedModeLabels = listOf(
                holder.displayNameLabelText,
                holder.displayNameText,
                holder.deviceIdLabelText,
                holder.deviceIdText,
                holder.deviceLastSeenLabelText,
                holder.deviceLastSeenText
        )
        if (detailedMode) {
            holder.summaryLabelText.isVisible = false

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

            detailedModeLabels.map {
                it.isVisible = true
                it.setTypeface(null, if (currentDevice) Typeface.BOLD else Typeface.NORMAL)
            }
        } else {
            holder.summaryLabelText.text =
                    span {
                        +(deviceInfo.displayName ?: deviceInfo.deviceId ?: "")
                        apply {
                            // Add additional info if current session is not trusted
                            if (!trustedSession) {
                                +"\n"
                                span {
                                    text = "${deviceInfo.deviceId}"
                                    apply {
                                        colorProvider?.getColorFromAttribute(R.attr.riotx_text_secondary)?.let {
                                            textColor = it
                                        }
                                        dimensionConverter?.spToPx(12)?.let {
                                            textSize = it
                                        }
                                    }
                                }
                            }
                        }
                    }

            holder.summaryLabelText.isVisible = true
            detailedModeLabels.map {
                it.isVisible = false
            }
        }
    }

    class Holder : VectorEpoxyHolder() {
        val root by bind<ViewGroup>(R.id.itemDeviceRoot)
        val summaryLabelText by bind<TextView>(R.id.itemDeviceSimpleSummary)
        val displayNameLabelText by bind<TextView>(R.id.itemDeviceDisplayNameLabel)
        val displayNameText by bind<TextView>(R.id.itemDeviceDisplayName)
        val deviceIdLabelText by bind<TextView>(R.id.itemDeviceIdLabel)
        val deviceIdText by bind<TextView>(R.id.itemDeviceId)
        val deviceLastSeenLabelText by bind<TextView>(R.id.itemDeviceLastSeenLabel)
        val deviceLastSeenText by bind<TextView>(R.id.itemDeviceLastSeen)

        val trustIcon by bind<ImageView>(R.id.itemDeviceTrustLevelIcon)
    }
}
