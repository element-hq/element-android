/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.algorithms.olm

import org.matrix.android.sdk.internal.crypto.MXOlmDevice
import org.matrix.android.sdk.internal.di.UserId
import javax.inject.Inject

internal class MXOlmDecryptionFactory @Inject constructor(
        private val olmDevice: MXOlmDevice,
        @UserId private val userId: String
) {

    fun create(): MXOlmDecryption {
        return MXOlmDecryption(
                olmDevice,
                userId
        )
    }
}
