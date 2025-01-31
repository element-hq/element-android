/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network.parsing

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import okhttp3.TlsVersion

internal class TlsVersionMoshiAdapter {

    @ToJson
    fun toJson(tlsVersion: TlsVersion): String {
        return tlsVersion.javaName
    }

    @FromJson
    fun fromJson(tlsVersionString: String): TlsVersion {
        return TlsVersion.forJavaName(tlsVersionString)
    }
}
