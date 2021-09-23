/*
 * Copyright (c) 2021 New Vector Ltd
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

package org.matrix.android.sdk.internal.crypto.dehydration

import org.matrix.android.sdk.internal.crypto.dehydration.model.ClaimDehydratedDeviceResponse
import org.matrix.android.sdk.internal.crypto.dehydration.model.DehydratedDevice
import org.matrix.android.sdk.internal.crypto.dehydration.model.DeviceDehydrationResponse
import org.matrix.android.sdk.internal.crypto.dehydration.model.GetDehydratedDeviceResponse
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

internal interface DehydrationApi {
    /**
     * set given device as dehydrated for the account.
     *
     * @param device data of the dehydrated device. Note that `deviceId` should be null at this point and `displayName` set.
     *
     * Ref: https://github.com/uhoreg/matrix-doc/blob/dehydration/proposals/2697-device-dehydration.md#dehydrating-a-device
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "org.matrix.msc2697.v2/dehydrated_device")
    suspend fun setDehydratedDevice(@Body device: DehydratedDevice): DeviceDehydrationResponse

    /**
     * Get current dehydrated device of the account.
     *
     * Ref: https://github.com/uhoreg/matrix-doc/blob/dehydration/proposals/2697-device-dehydration.md#rehydrating-a-device
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "org.matrix.msc2697.v2/dehydrated_device")
    suspend fun getDehydratedDevice(): GetDehydratedDeviceResponse

    /**
     * claim the current dehydrated device.
     *
     * Ref: https://github.com/uhoreg/matrix-doc/blob/dehydration/proposals/2697-device-dehydration.md#rehydrating-a-device
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "org.matrix.msc2697.v2/dehydrated_device/claim")
    suspend fun claimDehydratedDevice(deviceId: String): ClaimDehydratedDeviceResponse
}
