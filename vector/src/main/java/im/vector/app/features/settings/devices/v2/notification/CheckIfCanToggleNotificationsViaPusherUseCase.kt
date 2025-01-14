/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

class CheckIfCanToggleNotificationsViaPusherUseCase @Inject constructor() {

    fun execute(session: Session): Boolean {
        return session
                .homeServerCapabilitiesService()
                .getHomeServerCapabilities()
                .canRemotelyTogglePushNotificationsOfDevices
    }
}
