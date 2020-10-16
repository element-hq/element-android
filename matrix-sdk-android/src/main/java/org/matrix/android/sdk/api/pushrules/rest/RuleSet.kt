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
import org.matrix.android.sdk.api.pushrules.RuleIds
import org.matrix.android.sdk.api.pushrules.RuleSetKey

/**
 * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-pushrules
 */
@JsonClass(generateAdapter = true)
data class RuleSet(
        @Json(name = "content")
        val content: List<PushRule>? = null,
        @Json(name = "override")
        val override: List<PushRule>? = null,
        @Json(name = "room")
        val room: List<PushRule>? = null,
        @Json(name = "sender")
        val sender: List<PushRule>? = null,
        @Json(name = "underride")
        val underride: List<PushRule>? = null
) {
    fun getAllRules(): List<PushRule> {
        // Ref. for the order: https://matrix.org/docs/spec/client_server/latest#push-rules
        return override.orEmpty() + content.orEmpty() + room.orEmpty() + sender.orEmpty() + underride.orEmpty()
    }

    /**
     * Find a rule from its ruleID.
     *
     * @param ruleId a RULE_ID_XX value
     * @return the matched bing rule or null it doesn't exist.
     */
    fun findDefaultRule(ruleId: String?): PushRuleAndKind? {
        var result: PushRuleAndKind? = null
        // sanity check
        if (null != ruleId) {
            if (RuleIds.RULE_ID_CONTAIN_USER_NAME == ruleId) {
                result = findRule(content, ruleId)?.let { PushRuleAndKind(it, RuleSetKey.CONTENT) }
            } else {
                // assume that the ruleId is unique.
                result = findRule(override, ruleId)?.let { PushRuleAndKind(it, RuleSetKey.OVERRIDE) }
                if (null == result) {
                    result = findRule(underride, ruleId)?.let { PushRuleAndKind(it, RuleSetKey.UNDERRIDE) }
                }
            }
        }
        return result
    }

    /**
     * Find a rule from its rule Id.
     *
     * @param rules  the rules list.
     * @param ruleId the rule Id.
     * @return the bing rule if it exists, else null.
     */
    private fun findRule(rules: List<PushRule>?, ruleId: String): PushRule? {
        return rules?.firstOrNull { it.ruleId == ruleId }
    }
}

data class PushRuleAndKind(
        val pushRule: PushRule,
        val kind: RuleSetKey
)
