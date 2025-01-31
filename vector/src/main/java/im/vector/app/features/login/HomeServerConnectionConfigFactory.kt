/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.network.ssl.Fingerprint
import timber.log.Timber
import javax.inject.Inject

class HomeServerConnectionConfigFactory @Inject constructor() {

    fun create(url: String?, fingerprint: Fingerprint? = null): HomeServerConnectionConfig? {
        if (url == null) {
            return null
        }

        return try {
            HomeServerConnectionConfig.Builder()
                    .withHomeServerUri(url)
                    .run {
                        if (fingerprint == null) {
                            this
                        } else {
                            withAllowedFingerPrints(listOf(fingerprint))
                        }
                    }
                    .build()
        } catch (t: Throwable) {
            Timber.e(t)
            null
        }
    }
}
