/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.pushers

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.pushrules.rest.RuleSet

/**
 * All push rulesets for a user.
 * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-pushrules
 */
@JsonClass(generateAdapter = true)
internal data class GetPushRulesResponse(
        /**
         * Global rules, account level applying to all devices.
         */
        @Json(name = "global")
        val global: RuleSet,

        /**
         * Device specific rules, apply only to current device.
         */
        @Json(name = "device")
        val device: RuleSet? = null
)
