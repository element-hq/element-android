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

import com.google.gson.JsonObject;

import im.vector.matrix.android.internal.legacy.RestClient;
import im.vector.matrix.android.internal.legacy.rest.model.Versions;
import im.vector.matrix.android.internal.legacy.rest.model.login.LoginFlowResponse;
import im.vector.matrix.android.internal.legacy.rest.model.login.LoginParams;
import im.vector.matrix.android.internal.legacy.rest.model.login.RegistrationParams;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * The login REST API.
 */
public interface LoginApi {

    /**
     * Get the different login flows supported by the server.
     */
    @GET(RestClient.URI_API_PREFIX_PATH + "versions")
    Call<Versions> versions();

    /**
     * Get the different login flows supported by the server.
     */
    @GET(RestClient.URI_API_PREFIX_PATH_R0 + "login")
    Call<LoginFlowResponse> login();

    /**
     * Try to create an account
     *
     * @param params the registration params
     */
    @POST(RestClient.URI_API_PREFIX_PATH_R0 + "register")
    Call<JsonObject> register(@Body RegistrationParams params);

    /**
     * Pass params to the server for the current login phase.
     *
     * @param loginParams the login parameters
     */
    @POST(RestClient.URI_API_PREFIX_PATH_R0 + "login")
    Call<JsonObject> login(@Body LoginParams loginParams);

    /**
     * Invalidate the access token, so that it can no longer be used for authorization.
     */
    @POST(RestClient.URI_API_PREFIX_PATH_R0 + "logout")
    Call<JsonObject> logout();
}
