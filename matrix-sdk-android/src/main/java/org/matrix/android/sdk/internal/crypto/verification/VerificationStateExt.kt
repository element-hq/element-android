/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.verification

import org.matrix.android.sdk.api.session.crypto.verification.VerificationState
import org.matrix.android.sdk.api.session.crypto.verification.isCanceled

// State transition with control
internal fun VerificationState?.toState(newState: VerificationState): VerificationState {
    // Cancel is always prioritary ?
    // Eg id i found that mac or keys mismatch and send a cancel and the other send a done, i have to
    // consider as canceled
    if (newState.isCanceled()) {
        return newState
    }
    // never move out of cancel
    if (this?.isCanceled() == true) {
        return this
    }
    return newState
}
