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
package im.vector.matrix.android.internal.legacy.rest.client;

import im.vector.matrix.android.internal.auth.data.SessionParams;
import im.vector.matrix.android.internal.legacy.RestClient;
import im.vector.matrix.android.internal.legacy.rest.api.PushRulesApi;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.DefaultRetrofit2CallbackWrapper;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.BingRule;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.PushRulesResponse;

public class PushRulesRestClient extends RestClient<PushRulesApi> {

    /**
     * {@inheritDoc}
     */
    public PushRulesRestClient(SessionParams sessionParams) {
        super(sessionParams, PushRulesApi.class, RestClient.URI_API_PREFIX_PATH_R0, false);
    }

    /**
     * Retrieve the push rules list.
     *
     * @param callback the asynchronous callback.
     */
    public void getAllRules(final ApiCallback<PushRulesResponse> callback) {
        mApi.getAllRules().enqueue(new DefaultRetrofit2CallbackWrapper<>(callback));
    }

    /**
     * Update the rule enable status.
     *
     * @param Kind     the rule kind
     * @param ruleId   the rule id
     * @param status   the rule state
     * @param callback the asynchronous callback.
     */
    public void updateEnableRuleStatus(String Kind, String ruleId, boolean status, final ApiCallback<Void> callback) {
        mApi.updateEnableRuleStatus(Kind, ruleId, status).enqueue(new DefaultRetrofit2CallbackWrapper<>(callback));
    }

    /**
     * Update the rule actions lists.
     *
     * @param Kind     the rule kind
     * @param ruleId   the rule id
     * @param actions  the rule actions list
     * @param callback the asynchronous callback
     */
    public void updateRuleActions(String Kind, String ruleId, Object actions, final ApiCallback<Void> callback) {
        mApi.updateRuleActions(Kind, ruleId, actions).enqueue(new DefaultRetrofit2CallbackWrapper<>(callback));
    }

    /**
     * Delete a rule.
     *
     * @param Kind     the rule kind
     * @param ruleId   the rule id
     * @param callback the asynchronous callback
     */
    public void deleteRule(String Kind, String ruleId, final ApiCallback<Void> callback) {
        mApi.deleteRule(Kind, ruleId).enqueue(new DefaultRetrofit2CallbackWrapper<>(callback));
    }

    /**
     * Add a rule.
     *
     * @param rule     the rule
     * @param callback the asynchronous callback
     */
    public void addRule(BingRule rule, final ApiCallback<Void> callback) {
        mApi.addRule(rule.kind, rule.ruleId, rule.toJsonElement()).enqueue(new DefaultRetrofit2CallbackWrapper<>(callback));
    }
}
