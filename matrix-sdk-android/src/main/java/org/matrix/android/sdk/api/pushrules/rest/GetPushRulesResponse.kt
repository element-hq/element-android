/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.android.sdk.api.pushrules.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * All push rulesets for a user.
 * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-pushrules
 */
@JsonClass(generateAdapter = true)
internal data class GetPushRulesResponse(
        /**
         * Global rules, account level applying to all devices
         */
        @Json(name = "global")
        val global: RuleSet,

        /**
         * Device specific rules, apply only to current device
         */
        @Json(name = "device")
        val device: RuleSet? = null
)
