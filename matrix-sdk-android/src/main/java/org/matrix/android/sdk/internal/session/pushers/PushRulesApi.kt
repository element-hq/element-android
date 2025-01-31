/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.pushers

import org.matrix.android.sdk.api.session.pushrules.rest.PushRule
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface PushRulesApi {
    /**
     * Get all push rules.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushrules/")
    suspend fun getAllRules(): GetPushRulesResponse

    /**
     * Update the ruleID enable status.
     *
     * @param kind the notification kind (sender, room...)
     * @param ruleId the ruleId
     * @param enabledBody the new enable status
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushrules/global/{kind}/{ruleId}/enabled")
    suspend fun updateEnableRuleStatus(
            @Path("kind") kind: String,
            @Path("ruleId") ruleId: String,
            @Body enabledBody: EnabledBody
    )

    /**
     * Update the ruleID action.
     * Ref: https://matrix.org/docs/spec/client_server/latest#put-matrix-client-r0-pushrules-scope-kind-ruleid-actions
     *
     * @param kind the notification kind (sender, room...)
     * @param ruleId the ruleId
     * @param actions the actions
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushrules/global/{kind}/{ruleId}/actions")
    suspend fun updateRuleActions(
            @Path("kind") kind: String,
            @Path("ruleId") ruleId: String,
            @Body actions: Any
    )

    /**
     * Delete a rule.
     *
     * @param kind the notification kind (sender, room...)
     * @param ruleId the ruleId
     */
    @DELETE(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushrules/global/{kind}/{ruleId}")
    suspend fun deleteRule(
            @Path("kind") kind: String,
            @Path("ruleId") ruleId: String
    )

    /**
     * Add the ruleID enable status.
     *
     * @param kind the notification kind (sender, room...)
     * @param ruleId the ruleId.
     * @param rule the rule to add.
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushrules/global/{kind}/{ruleId}")
    suspend fun addRule(
            @Path("kind") kind: String,
            @Path("ruleId") ruleId: String,
            @Body rule: PushRule
    )
}
