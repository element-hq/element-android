/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.pushrules.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class PushRule(
        /**
         * Required. The actions to perform when this rule is matched.
         */
        val actions: List<Any>,
        /**
         * Required. Whether this is a default rule, or has been set explicitly.
         */
        val default: Boolean? = false,
        /**
         * Required. Whether the push rule is enabled or not.
         */
        val enabled: Boolean,
        /**
         * Required. The ID of this rule.
         */
        @Json(name = "rule_id") val ruleId: String,
        /**
         * The conditions that must hold true for an event in order for a rule to be applied to an event
         */
        val conditions: List<PushCondition>? = null,
        /**
         * The glob-style pattern to match against. Only applicable to content rules.
         */
        val pattern: String? = null
)

