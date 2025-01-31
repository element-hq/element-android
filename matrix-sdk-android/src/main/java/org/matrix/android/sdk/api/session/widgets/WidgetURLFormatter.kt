/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.widgets

interface WidgetURLFormatter {
    /**
     * Takes care of fetching a scalar token if required and build the final url.
     * This methods can throw, you should take care of handling failure.
     *
     * @param baseUrl the baseUrl which will be checked for scalar token
     * @param params additional params you want to append to the base url.
     * @param forceFetchScalarToken if true, you will force to fetch a new scalar token
     * from the server (only if the base url is whitelisted)
     * @param bypassWhitelist if true, the base url will be considered as whitelisted
     */
    suspend fun format(
            baseUrl: String,
            params: Map<String, String> = emptyMap(),
            forceFetchScalarToken: Boolean = false,
            bypassWhitelist: Boolean
    ): String
}
