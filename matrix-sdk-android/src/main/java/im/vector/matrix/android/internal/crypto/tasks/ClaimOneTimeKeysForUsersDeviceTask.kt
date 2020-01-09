/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.tasks

import im.vector.matrix.android.internal.crypto.api.CryptoApi
import im.vector.matrix.android.internal.crypto.model.MXKey
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.rest.KeysClaimBody
import im.vector.matrix.android.internal.crypto.model.rest.KeysClaimResponse
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import javax.inject.Inject

internal interface ClaimOneTimeKeysForUsersDeviceTask : Task<ClaimOneTimeKeysForUsersDeviceTask.Params, MXUsersDevicesMap<MXKey>> {
    data class Params(
            // a list of users, devices and key types to retrieve keys for.
            val usersDevicesKeyTypesMap: MXUsersDevicesMap<String>
    )
}

internal class DefaultClaimOneTimeKeysForUsersDevice @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val eventBus: EventBus
) : ClaimOneTimeKeysForUsersDeviceTask {

    override suspend fun execute(params: ClaimOneTimeKeysForUsersDeviceTask.Params): MXUsersDevicesMap<MXKey> {
        val body = KeysClaimBody(oneTimeKeys = params.usersDevicesKeyTypesMap.map)

        val keysClaimResponse = executeRequest<KeysClaimResponse>(eventBus) {
            apiCall = cryptoApi.claimOneTimeKeysForUsersDevices(body)
        }
        val map = MXUsersDevicesMap<MXKey>()
        keysClaimResponse.oneTimeKeys?.let { oneTimeKeys ->
            for ((userId, mapByUserId) in oneTimeKeys) {
                for ((deviceId, deviceKey) in mapByUserId) {
                    val mxKey = MXKey.from(deviceKey)

                    if (mxKey != null) {
                        map.setObject(userId, deviceId, mxKey)
                    } else {
                        Timber.e("## claimOneTimeKeysForUsersDevices : fail to create a MXKey")
                    }
                }
            }
        }
        return map
    }
}
