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
package im.vector.matrix.android.internal.crypto.api

import im.vector.matrix.android.internal.crypto.model.rest.*
import im.vector.matrix.android.internal.network.NetworkConstants
import retrofit2.Call
import retrofit2.http.*

internal interface CryptoApi {

    /**
     * Get the devices list
     * Doc: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-devices
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "devices")
    fun getDevices(): Call<DevicesListResponse>

    /**
     * Get the device info by id
     * Doc: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-devices-deviceid
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "devices/{deviceId}")
    fun getDeviceInfo(@Path("deviceId") deviceId: String): Call<DeviceInfo>

    /**
     * Upload device and/or one-time keys.
     * Doc: https://matrix.org/docs/spec/client_server/r0.4.0.html#post-matrix-client-r0-keys-upload
     *
     * @param params the params.
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "keys/upload")
    fun uploadKeys(@Body body: KeysUploadBody): Call<KeysUploadResponse>

    /**
     * Upload device and/or one-time keys.
     * Doc: not documented
     *
     * @param deviceId the deviceId
     * @param params   the params.
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "keys/upload/{deviceId}")
    fun uploadKeys(@Path("deviceId") deviceId: String, @Body body: KeysUploadBody): Call<KeysUploadResponse>

    /**
     * Download device keys.
     * Doc: https://matrix.org/docs/spec/client_server/r0.4.0.html#post-matrix-client-r0-keys-query
     *
     * @param params the params.
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "keys/query")
    fun downloadKeysForUsers(@Body params: KeysQueryBody): Call<KeysQueryResponse>

    /**
     * Claim one-time keys.
     * Doc: https://matrix.org/docs/spec/client_server/r0.4.0.html#post-matrix-client-r0-keys-claim
     *
     * @param params the params.
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "keys/claim")
    fun claimOneTimeKeysForUsersDevices(@Body body: KeysClaimBody): Call<KeysClaimResponse>

    /**
     * Send an event to a specific list of devices
     * Doc: https://matrix.org/docs/spec/client_server/r0.4.0.html#put-matrix-client-r0-sendtodevice-eventtype-txnid
     *
     * @param eventType     the type of event to send
     * @param transactionId the transaction ID for this event
     * @param body          the body
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "sendToDevice/{eventType}/{txnId}")
    fun sendToDevice(@Path("eventType") eventType: String, @Path("txnId") transactionId: String, @Body body: SendToDeviceBody): Call<Unit>

    /**
     * Delete a device.
     * Doc: https://matrix.org/docs/spec/client_server/r0.4.0.html#delete-matrix-client-r0-devices-deviceid
     *
     * @param deviceId the device id
     * @param params   the deletion parameters
     */
    @HTTP(path = NetworkConstants.URI_API_PREFIX_PATH_R0 + "devices/{device_id}", method = "DELETE", hasBody = true)
    fun deleteDevice(@Path("device_id") deviceId: String, @Body params: DeleteDeviceParams): Call<Unit>

    /**
     * Update the device information.
     * Doc: https://matrix.org/docs/spec/client_server/r0.4.0.html#put-matrix-client-r0-devices-deviceid
     *
     * @param deviceId the device id
     * @param params   the params
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "devices/{device_id}")
    fun updateDeviceInfo(@Path("device_id") deviceId: String, @Body params: UpdateDeviceInfoBody): Call<Unit>

    /**
     * Get the update devices list from two sync token.
     * Doc: https://matrix.org/docs/spec/client_server/r0.4.0.html#get-matrix-client-r0-keys-changes
     *
     * @param oldToken the start token.
     * @param newToken the up-to token.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "keys/changes")
    fun getKeyChanges(@Query("from") oldToken: String, @Query("to") newToken: String): Call<KeyChangesResponse>
}
