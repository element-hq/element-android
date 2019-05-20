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
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.internal.crypto.MXOlmDevice
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXKey
import im.vector.matrix.android.internal.crypto.model.MXOlmSessionResult
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.tasks.ClaimOneTimeKeysForUsersDeviceTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import timber.log.Timber
import java.util.*

internal class EnsureOlmSessionsForDevicesAction(private val mOlmDevice: MXOlmDevice,
                                                 private val mClaimOneTimeKeysForUsersDeviceTask: ClaimOneTimeKeysForUsersDeviceTask,
                                                 private val mTaskExecutor: TaskExecutor) {


    fun handle(devicesByUser: Map<String, List<MXDeviceInfo>>, callback: MatrixCallback<MXUsersDevicesMap<MXOlmSessionResult>>?) {
        val devicesWithoutSession = ArrayList<MXDeviceInfo>()

        val results = MXUsersDevicesMap<MXOlmSessionResult>()

        val userIds = devicesByUser.keys

        for (userId in userIds) {
            val deviceInfos = devicesByUser[userId]

            for (deviceInfo in deviceInfos!!) {
                val deviceId = deviceInfo.deviceId
                val key = deviceInfo.identityKey()

                val sessionId = mOlmDevice.getSessionId(key!!)

                if (TextUtils.isEmpty(sessionId)) {
                    devicesWithoutSession.add(deviceInfo)
                }

                val olmSessionResult = MXOlmSessionResult(deviceInfo, sessionId)
                results.setObject(olmSessionResult, userId, deviceId)
            }
        }

        if (devicesWithoutSession.size == 0) {
            callback?.onSuccess(results)
            return
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

        Timber.d("## claimOneTimeKeysForUsersDevices() : $usersDevicesToClaim")

        mClaimOneTimeKeysForUsersDeviceTask
                .configureWith(ClaimOneTimeKeysForUsersDeviceTask.Params(usersDevicesToClaim))
                .dispatchTo(object : MatrixCallback<MXUsersDevicesMap<MXKey>> {
                    override fun onSuccess(data: MXUsersDevicesMap<MXKey>) {
                        try {
                            Timber.d("## claimOneTimeKeysForUsersDevices() : keysClaimResponse.oneTimeKeys: $data")

                            for (userId in userIds) {
                                val deviceInfos = devicesByUser[userId]

                                for (deviceInfo in deviceInfos!!) {

                                    var oneTimeKey: MXKey? = null

                                    val deviceIds = data.getUserDeviceIds(userId)

                                    if (null != deviceIds) {
                                        for (deviceId in deviceIds) {
                                            val olmSessionResult = results.getObject(deviceId, userId)

                                            if (null != olmSessionResult!!.mSessionId) {
                                                // We already have a result for this device
                                                continue
                                            }

                                            val key = data.getObject(deviceId, userId)

                                            if (TextUtils.equals(key!!.type, oneTimeKeyAlgorithm)) {
                                                oneTimeKey = key
                                            }

                                            if (null == oneTimeKey) {
                                                Timber.d("## ensureOlmSessionsForDevices() : No one-time keys " + oneTimeKeyAlgorithm
                                                        + " for device " + userId + " : " + deviceId)
                                                continue
                                            }

                                            // Update the result for this device in results
                                            olmSessionResult.mSessionId = verifyKeyAndStartSession(oneTimeKey, userId, deviceInfo)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "## ensureOlmSessionsForDevices() " + e.message)
                        }

                        callback?.onSuccess(results)
                    }

                    override fun onFailure(failure: Throwable) {
                        Timber.e(failure, "## ensureOlmSessionsForUsers(): claimOneTimeKeysForUsersDevices request failed")

                        callback?.onFailure(failure)
                    }
                })
                .executeBy(mTaskExecutor)
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
                mOlmDevice.verifySignature(deviceInfo.fingerprint()!!, oneTimeKey.signalableJSONDictionary(), signature)
                isVerified = true
            } catch (e: Exception) {
                errorMessage = e.message
            }

            // Check one-time key signature
            if (isVerified) {
                sessionId = mOlmDevice.createOutboundSession(deviceInfo.identityKey()!!, oneTimeKey.value)

                if (!TextUtils.isEmpty(sessionId)) {
                    Timber.d("## verifyKeyAndStartSession() : Started new sessionid " + sessionId
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