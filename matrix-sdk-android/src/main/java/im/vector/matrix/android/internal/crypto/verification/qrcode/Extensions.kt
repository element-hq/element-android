/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.crypto.verification.qrcode

import im.vector.matrix.android.api.MatrixPatterns
import im.vector.matrix.android.api.permalinks.PermalinkFactory

/**
 * Generate an URL to generate a QR code of the form:
 * <pre>
 * https://matrix.to/#/<user-id>?
 *     request=<event-id>
 *     &action=verify
 *     &key_<keyid>=<key-in-base64>...
 *     &verification_algorithms=<algorithm>
 *     &verification_key=<random-key-in-base64>
 *     &other_user_key=<master-key-in-base64>
 * </pre>
 */
fun QrCodeData.toUrl(): String {
    return buildString {
        append(PermalinkFactory.createPermalink(userId))
        append("?request=")
        append(PermalinkFactory.escape(requestId))
        append("&action=verify")

        for ((keyId, key) in keys) {
            append("&key_$keyId=")
            append(PermalinkFactory.escape(key))
        }

        append("&verification_algorithms=")
        append(PermalinkFactory.escape(verificationAlgorithms))
        append("&verification_key=")
        append(PermalinkFactory.escape(verificationKey))
        append("&other_user_key=")
        append(PermalinkFactory.escape(otherUserKey))
    }
}

fun String.toQrCodeData(): QrCodeData? {
    if (!startsWith(PermalinkFactory.MATRIX_TO_URL_BASE)) {
        return null
    }

    val fragment = substringAfter("#")
    if (fragment.isEmpty()) {
        return null
    }

    val safeFragment = fragment.substringBefore("?")

    // we are limiting to 2 params
    val params = safeFragment
            .split(MatrixPatterns.SEP_REGEX.toRegex())
            .filter { it.isNotEmpty() }

    if (params.size != 1) {
        return null
    }

    val userId = params.getOrNull(0)
            ?.let { PermalinkFactory.unescape(it) }
            ?.takeIf { MatrixPatterns.isUserId(it) } ?: return null

    val urlParams = fragment.substringAfter("?")
            .split("&".toRegex())
            .filter { it.isNotEmpty() }

    val keyValues = urlParams.map {
        (it.substringBefore("=") to it.substringAfter("="))
    }.toMap()

    if (keyValues["action"] != "verify") {
        return null
    }

    val requestId = keyValues["request"]
            ?.let { PermalinkFactory.unescape(it) }
            ?.takeIf { MatrixPatterns.isEventId(it) } ?: return null
    val verificationAlgorithms = keyValues["verification_algorithms"] ?: return null
    val verificationKey = keyValues["verification_key"] ?: return null
    val otherUserKey = keyValues["other_user_key"] ?: return null

    val keys = keyValues.keys
            .filter { it.startsWith("key_") }
            .map {
                it.substringAfter("key_") to (keyValues[it] ?: return null)
            }
            .toMap()

    return QrCodeData(
            userId,
            requestId,
            keys,
            verificationAlgorithms,
            verificationKey,
            otherUserKey
    )
}
