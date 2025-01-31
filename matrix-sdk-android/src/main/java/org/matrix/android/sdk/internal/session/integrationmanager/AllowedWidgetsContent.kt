/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.integrationmanager

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AllowedWidgetsContent(
        /**
         * Map of stateEventId to Allowed.
         */
        @Json(name = "widgets") val widgets: Map<String, Boolean> = emptyMap(),

        /**
         * Map of native widgetType to a map of domain to Allowed.
         * <pre>
         * {
         *      "jitsi" : {
         *            "jitsi.domain.org"  : true,
         *            "jitsi.other.org"  : false
         *      }
         * }
         * </pre>
         */
        @Json(name = "native_widgets") val native: Map<String, Map<String, Boolean>> = emptyMap()
)
