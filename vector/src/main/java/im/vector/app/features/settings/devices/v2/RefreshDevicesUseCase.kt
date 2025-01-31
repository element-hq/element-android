/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import im.vector.app.core.di.ActiveSessionHolder
import org.matrix.android.sdk.api.NoOpMatrixCallback
import javax.inject.Inject

class RefreshDevicesUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {
    fun execute() {
        activeSessionHolder.getSafeActiveSession()?.let { session ->
            session.cryptoService().fetchDevicesList(NoOpMatrixCallback())
            session.cryptoService().downloadKeys(listOf(session.myUserId), true, NoOpMatrixCallback())
        }
    }
}
