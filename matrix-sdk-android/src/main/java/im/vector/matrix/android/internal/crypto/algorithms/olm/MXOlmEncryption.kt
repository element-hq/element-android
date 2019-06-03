/*
 * Copyright 2015 OpenMarket Ltd
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

package im.vector.matrix.android.internal.crypto.algorithms.olm

import android.text.TextUtils
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.internal.crypto.DeviceListManager
import im.vector.matrix.android.internal.crypto.MXOlmDevice
import im.vector.matrix.android.internal.crypto.actions.EnsureOlmSessionsForUsersAction
import im.vector.matrix.android.internal.crypto.actions.MessageEncrypter
import im.vector.matrix.android.internal.crypto.algorithms.IMXEncrypting
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXOlmSessionResult
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

internal class MXOlmEncryption(
        private var roomId: String,
        private val olmDevice: MXOlmDevice,
        private val cryptoStore: IMXCryptoStore,
        private val messageEncrypter: MessageEncrypter,
        private val deviceListManager: DeviceListManager,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val ensureOlmSessionsForUsersAction: EnsureOlmSessionsForUsersAction)
    : IMXEncrypting {

    override fun encryptEventContent(eventContent: Content,
                                     eventType: String,
                                     userIds: List<String>,
                                     callback: MatrixCallback<Content>) {
        // pick the list of recipients based on the membership list.
        //
        // TODO: there is a race condition here! What if a new user turns up
        CoroutineScope(coroutineDispatchers.crypto).launch {
            ensureSession(userIds, object : MatrixCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    val deviceInfos = ArrayList<MXDeviceInfo>()

                    for (userId in userIds) {
                        val devices = cryptoStore.getUserDevices(userId)?.values ?: emptyList()

                        for (device in devices) {
                            val key = device.identityKey()

                            if (TextUtils.equals(key, olmDevice.deviceCurve25519Key)) {
                                // Don't bother setting up session to ourself
                                continue
                            }

                            if (device.isBlocked) {
                                // Don't bother setting up sessions with blocked users
                                continue
                            }

                            deviceInfos.add(device)
                        }
                    }

                    val messageMap = HashMap<String, Any>()
                    messageMap["room_id"] = roomId
                    messageMap["type"] = eventType
                    messageMap["content"] = eventContent

                    messageEncrypter.encryptMessage(messageMap, deviceInfos)

                    callback.onSuccess(messageMap.toContent()!!)
                }
            })
        }
    }

    /**
     * Ensure that the session
     *
     * @param users    the user ids list
     * @param callback the asynchronous callback
     */
    private fun ensureSession(users: List<String>, callback: MatrixCallback<Unit>?) {
        deviceListManager.downloadKeys(users, false, object : MatrixCallback<MXUsersDevicesMap<MXDeviceInfo>> {

            override fun onSuccess(data: MXUsersDevicesMap<MXDeviceInfo>) {
                ensureOlmSessionsForUsersAction.handle(users, object : MatrixCallback<MXUsersDevicesMap<MXOlmSessionResult>> {
                    override fun onSuccess(data: MXUsersDevicesMap<MXOlmSessionResult>) {
                        callback?.onSuccess(Unit)
                    }

                    override fun onFailure(failure: Throwable) {
                        callback?.onFailure(failure)
                    }
                })
            }

            override fun onFailure(failure: Throwable) {
                callback?.onFailure(failure)
            }
        })
    }
}
