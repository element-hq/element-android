/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.failure

import org.matrix.android.sdk.api.network.ssl.Fingerprint

// This class will be sent to the bus
sealed class GlobalError {
    data class InvalidToken(val softLogout: Boolean) : GlobalError()
    data class ConsentNotGivenError(val consentUri: String) : GlobalError()
    data class CertificateError(val fingerprint: Fingerprint) : GlobalError()

    /**
     * The SDK requires the app (which should request the user) to perform an initial sync.
     */
    data class InitialSyncRequest(val reason: InitialSyncRequestReason) : GlobalError()
    object ExpiredAccount : GlobalError()
}
