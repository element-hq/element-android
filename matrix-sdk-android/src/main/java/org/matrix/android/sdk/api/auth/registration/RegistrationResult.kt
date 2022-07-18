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
