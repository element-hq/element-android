/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.flow.unwrap
import javax.inject.Inject

class CanToggleNotificationsViaPusherUseCase @Inject constructor() {

    fun execute(session: Session): Flow<Boolean> {
        return session
                .homeServerCapabilitiesService()
                .getHomeServerCapabilitiesLive()
                .asFlow()
                .unwrap()
                .map { it.canRemotelyTogglePushNotificationsOfDevices }
                .distinctUntilChanged()
    }
}
