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
import okhttp3.CipherSuite

internal class CipherSuiteMoshiAdapter {

    @ToJson
    fun toJson(cipherSuite: CipherSuite): String {
        return cipherSuite.javaName
    }

    @FromJson
    fun fromJson(cipherSuiteString: String): CipherSuite {
        return CipherSuite.forJavaName(cipherSuiteString)
    }
}
