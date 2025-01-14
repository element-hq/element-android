/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.device

import im.vector.app.core.di.ActiveSessionHolder
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import javax.inject.Inject

interface GetDeviceInfoUseCase {

    suspend fun execute(): CryptoDeviceInfo
}

class DefaultGetDeviceInfoUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder
) : GetDeviceInfoUseCase {

    override suspend fun execute(): CryptoDeviceInfo {
        return activeSessionHolder.getActiveSession().cryptoService().getMyCryptoDevice()
    }
}
