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
import java.net.URLDecoder
import java.net.URLEncoder

private const val ENCODING = "utf-8"

/**
 * Generate an URL to generate a QR code of the form:
 * <pre>
 * https://matrix.to/#/<user-id>?
 *     request=<event-id>
 *     &action=verify
 *     &key_<keyid>=<key-in-base64>...
 *     &secret=<shared_secret>
 *     &other_user_key=<master-key-in-base64>
 *
 * Example:
 * https://matrix.to/#/@user:matrix.org?
 *     request=%24pBeIfm7REDACTEDSQJbgqvi-yYiwmPB8_H_W_O974
 *     &action=verify
 *     &key_VJEDVKUYTQ=DL7LWIw7Qp%2B4AREDACTEDOwy2BjygumSWAGfzaWY
 *     &key_fsh%2FfQ08N3xvh4ySXsINB%2BJ2hREDACTEDVcVOG4qqo=fsh%2FfQ08N3xvh4ySXsINB%2BJ2hREDACTEDVcVOG4qqo
 *     &secret=AjQqw51Fp6UBuPolZ2FAD5WnXc22ZhJG6iGslrVvIdw%3D
 *     &other_user_key=WqSVLkBCS%2Fi5NqR%2F%2FymC8T7K9RPxBIuqK8Usl6Y3big
 * </pre>
 */
fun QrCodeData.toUrl(): String {
    return buildString {
        append(PermalinkFactory.createPermalink(userId))
        append("?request=")
        append(URLEncoder.encode(requestEventId, ENCODING))
        append("&action=")
        append(URLEncoder.encode(action, ENCODING))

        for ((keyId, key) in keys) {
            append("&key_${URLEncoder.encode(keyId, ENCODING)}=")
            append(URLEncoder.encode(key, ENCODING))
        }

        append("&secret=")
        append(URLEncoder.encode(sharedSecret, ENCODING))
        append("&other_user_key=")
        append(URLEncoder.encode(otherUserKey, ENCODING))
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
        (it.substringBefore("=") to it.substringAfter("=").let { value -> URLDecoder.decode(value, ENCODING) })
    }.toMap()

    val action = keyValues["action"] ?: return null

    val requestEventId = keyValues["request"]?.takeIf { MatrixPatterns.isEventId(it) } ?: return null
    val sharedSecret = keyValues["secret"] ?: return null
    val otherUserKey = keyValues["other_user_key"] ?: return null

    val keys = keyValues.keys
            .filter { it.startsWith("key_") }
            .map {
                URLDecoder.decode(it.substringAfter("key_"), ENCODING) to (keyValues[it] ?: return null)
            }
            .toMap()

    return QrCodeData(
            userId,
            requestEventId,
            action,
            keys,
            sharedSecret,
            otherUserKey
    )
}
