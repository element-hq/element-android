/*
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

import im.vector.matrix.android.internal.legacy.RestClient;
import im.vector.matrix.android.internal.legacy.rest.model.DeactivateAccountParams;
import im.vector.matrix.android.internal.legacy.rest.model.RequestEmailValidationParams;
import im.vector.matrix.android.internal.legacy.rest.model.RequestEmailValidationResponse;
import im.vector.matrix.android.internal.legacy.rest.model.RequestPhoneNumberValidationParams;
import im.vector.matrix.android.internal.legacy.rest.model.RequestPhoneNumberValidationResponse;
import im.vector.matrix.android.internal.legacy.rest.model.pid.AccountThreePidsResponse;
import im.vector.matrix.android.internal.legacy.rest.model.pid.AddThreePidsParams;
import im.vector.matrix.android.internal.legacy.rest.model.ChangePasswordParams;
import im.vector.matrix.android.internal.legacy.rest.model.pid.DeleteThreePidParams;
import im.vector.matrix.android.internal.legacy.rest.model.ForgetPasswordParams;
import im.vector.matrix.android.internal.legacy.rest.model.ForgetPasswordResponse;
import im.vector.matrix.android.internal.legacy.rest.model.User;
import im.vector.matrix.android.internal.legacy.rest.model.login.TokenRefreshParams;
import im.vector.matrix.android.internal.legacy.rest.model.login.TokenRefreshResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;


/**
 * The profile REST API.
 */
public interface ProfileApi {

    /**
     * Update a user's display name.
     *
     * @param userId the user id
     * @param user   the user object containing the new display name
     */
    @PUT(RestClient.URI_API_PREFIX_PATH_R0 + "profile/{userId}/displayname")
    Call<Void> displayname(@Path("userId") String userId, @Body User user);

    /**
     * Get a user's display name.
     *
     * @param userId the user id
     */
    @GET(RestClient.URI_API_PREFIX_PATH_R0 + "profile/{userId}/displayname")
    Call<User> displayname(@Path("userId") String userId);

    /**
     * Update a user's avatar URL.
     *
     * @param userId the user id
     * @param user   the user object containing the new avatar url
     */
    @PUT(RestClient.URI_API_PREFIX_PATH_R0 + "profile/{userId}/avatar_url")
    Call<Void> avatarUrl(@Path("userId") String userId, @Body User user);

    /**
     * Get a user's avatar URL.
     *
     * @param userId the user id
     */
    @GET(RestClient.URI_API_PREFIX_PATH_R0 + "profile/{userId}/avatar_url")
    Call<User> avatarUrl(@Path("userId") String userId);

    /**
     * Update the password
     *
     * @param passwordParams the new password
     */
    @POST(RestClient.URI_API_PREFIX_PATH_R0 + "account/password")
    Call<Void> updatePassword(@Body ChangePasswordParams passwordParams);

    /**
     * Reset the password server side.
     *
     * @param params the forget password params
     */
    @POST(RestClient.URI_API_PREFIX_PATH_R0 + "account/password/email/requestToken")
    Call<ForgetPasswordResponse> forgetPassword(@Body ForgetPasswordParams params);

    /**
     * Deactivate the user account
     *
     * @param params the deactivate account params
     */
    @POST(RestClient.URI_API_PREFIX_PATH_R0 + "account/deactivate")
    Call<Void> deactivate(@Body DeactivateAccountParams params);

    /**
     * Pass params to the server for the token refresh phase.
     *
     * @param refreshParams the refresh token parameters
     */
    @POST(RestClient.URI_API_PREFIX_PATH_R0 + "tokenrefresh")
    Call<TokenRefreshResponse> tokenrefresh(@Body TokenRefreshParams refreshParams);

    /**
     * List all 3PIDs linked to the Matrix user account.
     */
    @GET(RestClient.URI_API_PREFIX_PATH_R0 + "account/3pid")
    Call<AccountThreePidsResponse> threePIDs();

    /**
     * Add an 3Pid to a user
     *
     * @param params the params
     */
    @POST(RestClient.URI_API_PREFIX_PATH_R0 + "account/3pid")
    Call<Void> add3PID(@Body AddThreePidsParams params);

    /**
     * Delete a 3Pid of a user
     *
     * @param params the params
     */
    @POST(RestClient.URI_API_PREFIX_PATH_UNSTABLE + "account/3pid/delete")
    Call<Void> delete3PID(@Body DeleteThreePidParams params);

    /**
     * Request a validation token for an email
     * Note: Proxies the identity server API validate/email/requestToken, but first checks that
     * the given email address is not already associated with an account on this Home Server.
     *
     * @param params the parameters
     */
    @POST(RestClient.URI_API_PREFIX_PATH_R0 + "account/3pid/email/requestToken")
    Call<RequestEmailValidationResponse> requestEmailValidation(@Body RequestEmailValidationParams params);

    /**
     * Request a validation token for an email being added during registration process
     * Note: Proxies the identity server API validate/email/requestToken, but first checks that
     * the given email address is not already associated with an account on this Home Server.
     *
     * @param params the parameters
     */
    @POST(RestClient.URI_API_PREFIX_PATH_R0 + "register/email/requestToken")
    Call<RequestEmailValidationResponse> requestEmailValidationForRegistration(@Body RequestEmailValidationParams params);

    /**
     * Request a validation token for a phone number
     * Note: Proxies the identity server API validate/msisdn/requestToken, but first checks that
     * the given phone number is not already associated with an account on this Home Server.
     *
     * @param params the parameters
     */
    @POST(RestClient.URI_API_PREFIX_PATH_R0 + "account/3pid/msisdn/requestToken")
    Call<RequestPhoneNumberValidationResponse> requestPhoneNumberValidation(@Body RequestPhoneNumberValidationParams params);

    /**
     * Request a validation token for a phone number being added during registration process
     * Note: Proxies the identity server API validate/msisdn/requestToken, but first checks that
     * the given phone number is not already associated with an account on this Home Server.
     *
     * @param params the parameters
     */
    @POST(RestClient.URI_API_PREFIX_PATH_R0 + "register/msisdn/requestToken")
    Call<RequestPhoneNumberValidationResponse> requestPhoneNumberValidationForRegistration(@Body RequestPhoneNumberValidationParams params);
}
