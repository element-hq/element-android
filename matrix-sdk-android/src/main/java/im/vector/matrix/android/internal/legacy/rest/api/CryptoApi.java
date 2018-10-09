/*
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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


import im.vector.matrix.android.internal.legacy.rest.model.pid.DeleteDeviceParams;
import im.vector.matrix.android.internal.legacy.rest.model.sync.DevicesListResponse;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.KeyChangesResponse;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.KeysClaimResponse;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.KeysQueryResponse;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.KeysUploadResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CryptoApi {

    /**
     * Upload device and/or one-time keys.
     * @param params the params.
     */
    @POST("keys/upload")
    Call<KeysUploadResponse> uploadKeys(@Body Map<String, Object> params);

    /**
     * Upload device and/or one-time keys.
     *
     * @param deviceId the deviceId
     * @param params   the params.
     */
    @POST("keys/upload/{deviceId}")
    Call<KeysUploadResponse> uploadKeys(@Path("deviceId") String deviceId, @Body Map<String, Object> params);

    /**
     * Download device keys.
     * @param params the params.
     */
    @POST("keys/query")
    Call<KeysQueryResponse> downloadKeysForUsers(@Body Map<String, Object> params);

    /**
     * Claim one-time keys.
     * @param params the params.
     */
    @POST("keys/claim")
    Call<KeysClaimResponse> claimOneTimeKeysForUsersDevices(@Body Map<String, Object> params);

    /**
     * Send an event to a specific list of devices
     *
     * @param eventType     the type of event to send
     * @param transactionId the random path item
     * @param params        the params
     */
    @PUT("sendToDevice/{eventType}/{random}")
    Call<Void> sendToDevice(@Path("eventType") String eventType, @Path("random") String transactionId, @Body Map<String, Object> params);

    /**
     * Get the devices list
     */
    @GET("devices")
    Call<DevicesListResponse> getDevices();

    /**
     * Delete a device.
     *
     * @param deviceId the device id
     * @param params   the deletion parameters
     */
    @HTTP(path = "devices/{device_id}", method = "DELETE", hasBody = true)
    Call<Void> deleteDevice(@Path("device_id") String deviceId, @Body DeleteDeviceParams params);

    /**
     * Update the device information.
     *
     * @param deviceId the device id
     * @param params   the params
     */
    @PUT("devices/{device_id}")
    Call<Void> updateDeviceInfo(@Path("device_id") String deviceId, @Body Map<String, String> params);

    /**
     * Get the update devices list from two sync token.
     *
     * @param oldToken the start token.
     * @param newToken the up-to token.
     */
    @GET("keys/changes")
    Call<KeyChangesResponse> getKeyChanges(@Query("from") String oldToken, @Query("to") String newToken);
}
