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
package org.matrix.android.sdk.internal.session.pushers

import org.matrix.android.sdk.api.pushrules.rest.GetPushRulesResponse
import org.matrix.android.sdk.api.pushrules.rest.PushRule
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface PushRulesApi {
    /**
     * Get all push rules
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushrules/")
    suspend fun getAllRules(): GetPushRulesResponse

    /**
     * Update the ruleID enable status
     *
     * @param kind   the notification kind (sender, room...)
     * @param ruleId the ruleId
     * @param enable the new enable status
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushrules/global/{kind}/{ruleId}/enabled")
    suspend fun updateEnableRuleStatus(@Path("kind") kind: String,
                                       @Path("ruleId") ruleId: String,
                                       @Body enabledBody: EnabledBody)

    /**
     * Update the ruleID action
     * Ref: https://matrix.org/docs/spec/client_server/latest#put-matrix-client-r0-pushrules-scope-kind-ruleid-actions
     *
     * @param kind    the notification kind (sender, room...)
     * @param ruleId  the ruleId
     * @param actions the actions
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushrules/global/{kind}/{ruleId}/actions")
    suspend fun updateRuleActions(@Path("kind") kind: String,
                                  @Path("ruleId") ruleId: String,
                                  @Body actions: Any)

    /**
     * Delete a rule
     *
     * @param kind   the notification kind (sender, room...)
     * @param ruleId the ruleId
     */
    @DELETE(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushrules/global/{kind}/{ruleId}")
    suspend fun deleteRule(@Path("kind") kind: String,
                           @Path("ruleId") ruleId: String)

    /**
     * Add the ruleID enable status
     *
     * @param kind   the notification kind (sender, room...)
     * @param ruleId the ruleId.
     * @param rule   the rule to add.
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushrules/global/{kind}/{ruleId}")
    suspend fun addRule(@Path("kind") kind: String,
                        @Path("ruleId") ruleId: String,
                        @Body rule: PushRule)
}
