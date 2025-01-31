/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.crosssigning

import android.util.Base64
import org.matrix.android.sdk.api.session.crypto.crosssigning.CryptoCrossSigningKey
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import timber.log.Timber

internal fun CryptoDeviceInfo.canonicalSignable(): String {
    return JsonCanonicalizer.getCanonicalJson(Map::class.java, signalableJSONDictionary())
}

internal fun CryptoCrossSigningKey.canonicalSignable(): String {
    return JsonCanonicalizer.getCanonicalJson(Map::class.java, signalableJSONDictionary())
}

/**
 * Decode the base 64. Return null in case of bad format. Should be used when parsing received data from external source
 */
internal fun String.fromBase64Safe(): ByteArray? {
    return try {
        Base64.decode(this, Base64.DEFAULT)
    } catch (throwable: Throwable) {
        Timber.e(throwable, "Unable to decode base64 string")
        null
    }
}
