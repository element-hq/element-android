/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.algorithms.olm

import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.internal.crypto.DeviceListManager
import org.matrix.android.sdk.internal.crypto.MXOlmDevice
import org.matrix.android.sdk.internal.crypto.actions.EnsureOlmSessionsForUsersAction
import org.matrix.android.sdk.internal.crypto.actions.MessageEncrypter
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import javax.inject.Inject

internal class MXOlmEncryptionFactory @Inject constructor(
        private val olmDevice: MXOlmDevice,
        private val cryptoStore: IMXCryptoStore,
        private val messageEncrypter: MessageEncrypter,
        private val deviceListManager: DeviceListManager,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val ensureOlmSessionsForUsersAction: EnsureOlmSessionsForUsersAction
) {

    fun create(roomId: String): MXOlmEncryption {
        return MXOlmEncryption(
                roomId,
                olmDevice,
                cryptoStore,
                messageEncrypter,
                deviceListManager,
                ensureOlmSessionsForUsersAction
        )
    }
}
