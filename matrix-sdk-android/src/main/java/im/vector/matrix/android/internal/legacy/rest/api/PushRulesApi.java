/*
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2018 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.matrix.android.internal.legacy.rest.api;

import com.google.gson.JsonElement;

import im.vector.matrix.android.internal.legacy.rest.model.bingrules.PushRulesResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface PushRulesApi {

    /**
     * Get all push rules
     */
    @GET("pushrules/")
    Call<PushRulesResponse> getAllRules();

    /**
     * Update the ruleID enable status
     *
     * @param kind   the notification kind (sender, room...)
     * @param ruleId the ruleId
     * @param enable the new enable status
     */
    @PUT("pushrules/global/{kind}/{ruleId}/enabled")
    Call<Void> updateEnableRuleStatus(@Path("kind") String kind, @Path("ruleId") String ruleId, @Body Boolean enable);


    /**
     * Update the ruleID enable status
     *
     * @param kind    the notification kind (sender, room...)
     * @param ruleId  the ruleId
     * @param actions the actions
     */
    @PUT("pushrules/global/{kind}/{ruleId}/actions")
    Call<Void> updateRuleActions(@Path("kind") String kind, @Path("ruleId") String ruleId, @Body Object actions);


    /**
     * Update the ruleID enable status
     *
     * @param kind   the notification kind (sender, room...)
     * @param ruleId the ruleId
     */
    @DELETE("pushrules/global/{kind}/{ruleId}")
    Call<Void> deleteRule(@Path("kind") String kind, @Path("ruleId") String ruleId);

    /**
     * Add the ruleID enable status
     *
     * @param kind   the notification kind (sender, room...)
     * @param ruleId the ruleId.
     * @param rule   the rule to add.
     */
    @PUT("pushrules/global/{kind}/{ruleId}")
    Call<Void> addRule(@Path("kind") String kind, @Path("ruleId") String ruleId, @Body JsonElement rule);
}
