/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface SettingsActivityPayload : Parcelable {

    @Parcelize object Root : SettingsActivityPayload
    @Parcelize object AdvancedSettings : SettingsActivityPayload
    @Parcelize object SecurityPrivacy : SettingsActivityPayload
    @Parcelize object SecurityPrivacyManageSessions : SettingsActivityPayload
    @Parcelize object General : SettingsActivityPayload
    @Parcelize object Notifications : SettingsActivityPayload

    @Parcelize
    data class DiscoverySettings(val expandIdentityPolicies: Boolean = false) : SettingsActivityPayload
}
