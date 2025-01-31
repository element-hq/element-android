/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.auth.wellknown

import org.matrix.android.sdk.api.auth.data.WellKnown

/**
 * Ref: https://matrix.org/docs/spec/client_server/latest#well-known-uri
 */
sealed class WellknownResult {
    /**
     * Retrieve the specific piece of information from the user in a way which fits within the existing client user experience,
     * if the client is inclined to do so. Failure can take place instead if no good user experience for this is possible at this point.
     */
    data class Prompt(
            val homeServerUrl: String,
            val identityServerUrl: String?,
            val wellKnown: WellKnown
    ) : WellknownResult()

    /**
     * Stop the current auto-discovery mechanism. If no more auto-discovery mechanisms are available,
     * then the client may use other methods of determining the required parameters, such as prompting the user, or using default values.
     */
    object Ignore : WellknownResult()

    /**
     * Inform the user that auto-discovery failed due to invalid/empty data and PROMPT for the parameter.
     */
    data class FailPrompt(val homeServerUrl: String?, val wellKnown: WellKnown?) : WellknownResult()

    /**
     * Inform the user that auto-discovery did not return any usable URLs. Do not continue further with the current login process.
     * At this point, valid data was obtained, but no homeserver is available to serve the client.
     * No further guess should be attempted and the user should make a conscientious decision what to do next.
     */
    object FailError : WellknownResult()
}
