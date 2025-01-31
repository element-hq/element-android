/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.worker

import androidx.work.OneTimeWorkRequest
import org.matrix.android.sdk.internal.session.room.send.NoMerger

/**
 * If startChain parameter is true, the builder will have a inputMerger set to [NoMerger].
 */
internal fun OneTimeWorkRequest.Builder.startChain(startChain: Boolean): OneTimeWorkRequest.Builder {
    if (startChain) {
        setInputMerger(NoMerger::class.java)
    }
    return this
}
