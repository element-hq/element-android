/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.keysbackup.model

import org.matrix.android.sdk.api.util.JsonDict

internal data class SignalableMegolmBackupAuthData(
        val publicKey: String,
        val privateKeySalt: String? = null,
        val privateKeyIterations: Int? = null
) {
    fun signalableJSONDictionary(): JsonDict = HashMap<String, Any>().apply {
        put("public_key", publicKey)

        privateKeySalt?.let {
            put("private_key_salt", it)
        }
        privateKeyIterations?.let {
            put("private_key_iterations", it)
        }
    }
}
