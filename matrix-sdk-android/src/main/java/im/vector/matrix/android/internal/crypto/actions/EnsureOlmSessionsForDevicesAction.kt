/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.actions

import android.text.TextUtils
import arrow.core.Try
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.internal.crypto.MXOlmDevice
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXKey
import im.vector.matrix.android.internal.crypto.model.MXOlmSessionResult
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.tasks.ClaimOneTimeKeysForUsersDeviceTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import timber.log.Timber
import java.util.*

internal class EnsureOlmSessionsForDevicesAction(private val olmDevice: MXOlmDevice,
                                                 private val oneTimeKeysForUsersDeviceTask: ClaimOneTimeKeysForUsersDeviceTask) {


    suspend fun handle(devicesByUser: Map<String, List<MXDeviceInfo>>): Try<MXUsersDevicesMap<MXOlmSessionResult>> {
        val devicesWithoutSession = ArrayList<MXDeviceInfo>()

        val results = MXUsersDevicesMap<MXOlmSessionResult>()

        val userIds = devicesByUser.keys

        for (userId in userIds) {
            val deviceInfos = devicesByUser[userId]

            for (deviceInfo in deviceInfos!!) {
                val deviceId = deviceInfo.deviceId
                val key = deviceInfo.identityKey()

                val sessionId = olmDevice.getSessionId(key!!)

                if (TextUtils.isEmpty(sessionId)) {
                    devicesWithoutSession.add(deviceInfo)
                }

                val olmSessionResult = MXOlmSessionResult(deviceInfo, sessionId)
                results.setObject(olmSessionResult, userId, deviceId)
            }
        }

        if (devicesWithoutSession.size == 0) {
            return Try.just(results)
        }

        // Prepare the request for claiming one-time keys
        val usersDevicesToClaim = MXUsersDevicesMap<String>()

        val oneTimeKeyAlgorithm = MXKey.KEY_SIGNED_CURVE_25519_TYPE

        for (device in devicesWithoutSession) {
            usersDevicesToClaim.setObject(oneTimeKeyAlgorithm, device.userId, device.deviceId)
        }

        // TODO: this has a race condition - if we try to send another message
        // while we are claiming a key, we will end up claiming two and setting up
        // two sessions.
        //
        // That should eventually resolve itself, but it's poor form.

        Timber.v("## claimOneTimeKeysForUsersDevices() : $usersDevicesToClaim")

        val claimParams = ClaimOneTimeKeysForUsersDeviceTask.Params(usersDevicesToClaim)
        return oneTimeKeysForUsersDeviceTask
                .execute(claimParams)
                .map {
                    Timber.v("## claimOneTimeKeysForUsersDevices() : keysClaimResponse.oneTimeKeys: $it")
                    for (userId in userIds) {
                        val deviceInfos = devicesByUser[userId]
                        for (deviceInfo in deviceInfos!!) {
                            var oneTimeKey: MXKey? = null
                            val deviceIds = it.getUserDeviceIds(userId)
                            if (null != deviceIds) {
                                for (deviceId in deviceIds) {
                                    val olmSessionResult = results.getObject(deviceId, userId)
                                    if (olmSessionResult!!.mSessionId != null) {
                                        // We already have a result for this device
                                        continue
                                    }
                                    val key = it.getObject(deviceId, userId)
                                    if (key?.type == oneTimeKeyAlgorithm) {
                                        oneTimeKey = key
                                    }
                                    if (oneTimeKey == null) {
                                        Timber.v("## ensureOlmSessionsForDevices() : No one-time keys " + oneTimeKeyAlgorithm
                                                + " for device " + userId + " : " + deviceId)
                                        continue
                                    }
                                    // Update the result for this device in results
                                    olmSessionResult.mSessionId = verifyKeyAndStartSession(oneTimeKey, userId, deviceInfo)
                                }
                            }
                        }
                    }
                    results
                }
    }

    private fun verifyKeyAndStartSession(oneTimeKey: MXKey, userId: String, deviceInfo: MXDeviceInfo): String? {
        var sessionId: String? = null

        val deviceId = deviceInfo.deviceId
        val signKeyId = "ed25519:$deviceId"
        val signature = oneTimeKey.signatureForUserId(userId, signKeyId)

        if (!TextUtils.isEmpty(signature) && !TextUtils.isEmpty(deviceInfo.fingerprint())) {
            var isVerified = false
            var errorMessage: String? = null

            try {
                olmDevice.verifySignature(deviceInfo.fingerprint()!!, oneTimeKey.signalableJSONDictionary(), signature)
                isVerified = true
            } catch (e: Exception) {
                errorMessage = e.message
            }

            // Check one-time key signature
            if (isVerified) {
                sessionId = olmDevice.createOutboundSession(deviceInfo.identityKey()!!, oneTimeKey.value)

                if (!TextUtils.isEmpty(sessionId)) {
                    Timber.v("## verifyKeyAndStartSession() : Started new sessionid " + sessionId
                            + " for device " + deviceInfo + "(theirOneTimeKey: " + oneTimeKey.value + ")")
                } else {
                    // Possibly a bad key
                    Timber.e("## verifyKeyAndStartSession() : Error starting session with device $userId:$deviceId")
                }
            } else {
                Timber.e("## verifyKeyAndStartSession() : Unable to verify signature on one-time key for device " + userId
                        + ":" + deviceId + " Error " + errorMessage)
            }
        }

        return sessionId
    }

}