/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network.ssl

import android.os.Build
import org.matrix.android.sdk.api.network.ssl.Fingerprint
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

internal object PinnedTrustManagerProvider {
    // Set to false to perform some tests
    private const val USE_DEFAULT_TRUST_MANAGER = true

    fun provide(
            fingerprints: List<Fingerprint>?,
            defaultTrustManager: X509TrustManager?
    ): X509TrustManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && defaultTrustManager is X509ExtendedTrustManager) {
            PinnedTrustManagerApi24(
                    fingerprints.orEmpty(),
                    defaultTrustManager.takeIf { USE_DEFAULT_TRUST_MANAGER }
            )
        } else {
            PinnedTrustManager(
                    fingerprints.orEmpty(),
                    defaultTrustManager.takeIf { USE_DEFAULT_TRUST_MANAGER }
            )
        }
    }
}
