package im.vector.riotx.features.roommemberprofile.devices

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.errorWithRetryItem
import im.vector.riotx.core.epoxy.loadingItem
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.resources.UserPreferencesProvider
import im.vector.riotx.core.ui.list.GenericItem
import im.vector.riotx.core.ui.list.genericFooterItem
import im.vector.riotx.core.ui.list.genericItem
import im.vector.riotx.core.ui.list.genericItemWithValue
import im.vector.riotx.features.settings.VectorPreferences
import javax.inject.Inject

class DeviceListEpoxyController @Inject constructor(private val stringProvider: StringProvider,
                                                    private val colorProvider: ColorProvider,
                                                    private val vectorPreferences: VectorPreferences)
    : TypedEpoxyController<DeviceListViewState>() {

    interface InteractionListener {
        fun onDeviceSelected(device: CryptoDeviceInfo)
    }

    var interactionListener: InteractionListener? = null

    override fun buildModels(data: DeviceListViewState?) {
        if (data == null) {
            return
        }
        when (data.cryptoDevices) {
            Uninitialized -> {

            }
            is Loading    -> {
                loadingItem {
                    id("loading")
                    loadingText(stringProvider.getString(R.string.loading))
                }
            }
            is Success    -> {

                val deviceList = data.cryptoDevices.invoke()

                // Build top header
                val allGreen = deviceList.fold(true, { prev, device ->
                    prev && device.isVerified
                })

                genericItem {
                    id("title")
                    style(GenericItem.STYLE.BIG_TEXT)
                    titleIconResourceId(if (allGreen) R.drawable.ic_shield_trusted else R.drawable.ic_shield_warning)
                    title(stringProvider.getString(R.string.verification_profile_verified))
                    description(stringProvider.getString(R.string.verification_conclusion_ok_notice))
                }

                genericItem {
                    id("sessions")
                    style(GenericItem.STYLE.BIG_TEXT)
                    title(stringProvider.getString(R.string.room_member_profile_sessions_section_title))

                }
                if (deviceList.isEmpty()) {
                    // Can this really happen?
                    genericFooterItem {
                        id("empty")
                        text(stringProvider.getString(R.string.search_no_results))
                    }
                } else {
                    // Build list of device with status
                    deviceList.forEach { device ->
                        genericItemWithValue {
                            id(device.deviceId)
                            titleIconResourceId(if (device.isVerified) R.drawable.ic_shield_trusted else R.drawable.ic_shield_warning)
                            title(
                                    buildString {
                                        append(device.displayName() ?: device.deviceId)
                                        apply {
                                            if (vectorPreferences.developerMode()) {
                                                append("\n")
                                                append(device.deviceId)
                                            }
                                        }
                                    }

                            )
                            value(
                                    stringProvider.getString(
                                            if (device.isVerified) R.string.trusted else R.string.not_trusted
                                    )
                            )
                            valueColorInt(
                                    colorProvider.getColor(
                                            if (device.isVerified) R.color.riotx_positive_accent else R.color.riotx_destructive_accent
                                    )
                            )
                        }
                    }
                }
            }
            is Fail       -> {
                errorWithRetryItem {
                    id("error")
                    text(stringProvider.getString(R.string.room_member_profile_failed_to_get_devices))
                    listener {
                        // TODO
                    }
                }
            }
        }
    }
}
