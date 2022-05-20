/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
