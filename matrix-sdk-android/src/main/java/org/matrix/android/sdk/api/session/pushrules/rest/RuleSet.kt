/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.pushrules.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.pushrules.RuleIds
import org.matrix.android.sdk.api.session.pushrules.RuleSetKey

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
     * @param rules the rules list.
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
