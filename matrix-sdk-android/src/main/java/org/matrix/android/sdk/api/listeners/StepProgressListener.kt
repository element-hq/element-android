/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.listeners

/**
 * Interface to send a progress info.
 */
interface StepProgressListener {

    sealed class Step {
        data class ComputingKey(val progress: Int, val total: Int) : Step()
        object DownloadingKey : Step()
        data class ImportingKey(val progress: Int, val total: Int) : Step()
    }

    /**
     * @param step The current step, containing progress data if available. Else you should consider progress as indeterminate
     */
    fun onStepProgress(step: Step)
}
