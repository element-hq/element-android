/*
 * Copyright 2015 OpenMarket Ltd
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

import com.google.gson.JsonObject;

import im.vector.matrix.android.internal.legacy.HomeServerConnectionConfig;
import im.vector.matrix.android.internal.legacy.RestClient;
import im.vector.matrix.android.internal.legacy.rest.api.CallRulesApi;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.DefaultRetrofit2CallbackWrapper;

public class CallRestClient extends RestClient<CallRulesApi> {

    /**
     * {@inheritDoc}
     */
    public CallRestClient(HomeServerConnectionConfig hsConfig) {
        super(hsConfig, CallRulesApi.class, RestClient.URI_API_PREFIX_PATH_R0, false);
    }

    public void getTurnServer(final ApiCallback<JsonObject> callback) {
        mApi.getTurnServer().enqueue(new DefaultRetrofit2CallbackWrapper<>(callback));
    }
}
