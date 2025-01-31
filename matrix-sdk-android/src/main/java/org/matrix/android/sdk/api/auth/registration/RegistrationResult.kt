/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.auth.registration

import org.matrix.android.sdk.api.session.Session

/**
 * Either a session or an object containing data about registration stages.
 */
sealed class RegistrationResult {
    /**
     * The registration is successful, the [Session] is provided.
     */
    data class Success(val session: Session) : RegistrationResult()

    /**
     * The registration still miss some steps. See [FlowResult] to know the details.
     */
    data class FlowResponse(val flowResult: FlowResult) : RegistrationResult()
}

/**
 * Information about the missing and completed [Stage].
 */
data class FlowResult(
        /**
         * List of missing stages.
         */
        val missingStages: List<Stage>,
        /**
         * List of completed stages.
         */
        val completedStages: List<Stage>
)
