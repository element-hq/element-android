/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto

import org.matrix.android.sdk.api.auth.data.Credentials
import javax.inject.Inject

internal class ObjectSigner @Inject constructor(
        private val credentials: Credentials,
        private val olmDevice: MXOlmDevice
) {

    /**
     * Sign Object.
     *
     * Example:
     * <pre>
     *     {
     *         "[MY_USER_ID]": {
     *             "ed25519:[MY_DEVICE_ID]": "sign(str)"
     *         }
     *     }
     * </pre>
     *
     * @param strToSign the String to sign and to include in the Map
     * @return a Map (see example)
     */
    fun signObject(strToSign: String): Map<String, Map<String, String>> {
        val result = HashMap<String, Map<String, String>>()

        val content = HashMap<String, String>()

        content["ed25519:" + credentials.deviceId] = olmDevice.signMessage(strToSign)
                ?: "" // null reported by rageshake if happens during logout

        result[credentials.userId] = content

        return result
    }
}
