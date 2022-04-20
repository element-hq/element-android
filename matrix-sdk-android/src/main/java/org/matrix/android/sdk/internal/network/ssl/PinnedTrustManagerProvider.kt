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

package org.matrix.android.sdk.internal.network.ssl

import android.os.Build
import org.matrix.android.sdk.api.network.ssl.Fingerprint
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

internal object PinnedTrustManagerProvider {
    // Set to false to perform some tests
    private const val USE_DEFAULT_TRUST_MANAGER = true

    fun provide(fingerprints: List<Fingerprint>?,
                defaultTrustManager: X509TrustManager?): X509TrustManager {
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
