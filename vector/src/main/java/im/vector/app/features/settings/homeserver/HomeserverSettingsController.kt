/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.homeserver

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericWithValueItem
import im.vector.app.features.discovery.settingsCenteredImageItem
import im.vector.app.features.discovery.settingsInfoItem
import im.vector.app.features.discovery.settingsSectionTitleItem
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.federation.FederationVersion
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.homeserver.RoomVersionStatus
import javax.inject.Inject

class HomeserverSettingsController @Inject constructor(
        private val stringProvider: StringProvider,
        private val errorFormatter: ErrorFormatter,
        private val vectorPreferences: VectorPreferences
) : TypedEpoxyController<HomeServerSettingsViewState>() {

    var callback: Callback? = null

    interface Callback {
        fun retry()
    }

    override fun buildModels(data: HomeServerSettingsViewState?) {
        data ?: return
        val host = this

        buildHeader(data)
        buildCapabilities(data)
        when (val federationVersion = data.federationVersion) {
            is Loading,
            is Uninitialized ->
                loadingItem {
                    id("loading")
                }
            is Fail ->
                errorWithRetryItem {
                    id("error")
                    text(host.errorFormatter.toHumanReadable(federationVersion.error))
                    listener { host.callback?.retry() }
                }
            is Success ->
                buildFederationVersion(federationVersion())
        }
    }

    private fun buildHeader(state: HomeServerSettingsViewState) {
        settingsCenteredImageItem {
            id("icon")
            drawableRes(R.drawable.ic_layers)
        }
        settingsSectionTitleItem {
            id("urlTitle")
            titleResId(CommonStrings.hs_url)
        }
        settingsInfoItem {
            id("urlValue")
            helperText(state.homeserverUrl)
        }
        if (vectorPreferences.developerMode()) {
            settingsSectionTitleItem {
                id("urlApiTitle")
                titleResId(CommonStrings.hs_client_url)
            }
            settingsInfoItem {
                id("urlApiValue")
                helperText(state.homeserverClientServerApiUrl)
            }
        }
    }

    private fun buildFederationVersion(federationVersion: FederationVersion) {
        settingsSectionTitleItem {
            id("nameTitle")
            titleResId(CommonStrings.settings_server_name)
        }
        settingsInfoItem {
            id("nameValue")
            helperText(federationVersion.name)
        }
        settingsSectionTitleItem {
            id("versionTitle")
            titleResId(CommonStrings.settings_server_version)
        }
        settingsInfoItem {
            id("versionValue")
            helperText(federationVersion.version)
        }
    }

    private fun buildCapabilities(data: HomeServerSettingsViewState) {
        val host = this
        settingsSectionTitleItem {
            id("uploadTitle")
            titleResId(CommonStrings.settings_server_upload_size_title)
        }

        val limit = data.homeServerCapabilities.maxUploadFileSize

        settingsInfoItem {
            id("uploadValue")
            if (limit == HomeServerCapabilities.MAX_UPLOAD_FILE_SIZE_UNKNOWN) {
                helperTextResId(CommonStrings.settings_server_upload_size_unknown)
            } else {
                helperText(host.stringProvider.getString(CommonStrings.settings_server_upload_size_content, "${limit / 1048576L} MB"))
            }
        }

        if (vectorPreferences.developerMode()) {
            val roomCapabilities = data.homeServerCapabilities.roomVersions
            if (roomCapabilities != null) {
                settingsSectionTitleItem {
                    id("room_versions")
                    titleResId(CommonStrings.settings_server_room_versions)
                }

                genericWithValueItem {
                    id("room_version_default")
                    title(host.stringProvider.getString(CommonStrings.settings_server_default_room_version).toEpoxyCharSequence())
                    value(roomCapabilities.defaultRoomVersion)
                }

                roomCapabilities.supportedVersion.forEach {
                    genericWithValueItem {
                        id("room_version_${it.version}")
                        title(it.version.toEpoxyCharSequence())
                        value(
                                host.stringProvider.getString(
                                        when (it.status) {
                                            RoomVersionStatus.STABLE -> CommonStrings.settings_server_room_version_stable
                                            RoomVersionStatus.UNSTABLE -> CommonStrings.settings_server_room_version_unstable
                                        }
                                )
                        )
                    }
                }
            }
        }
    }
}
