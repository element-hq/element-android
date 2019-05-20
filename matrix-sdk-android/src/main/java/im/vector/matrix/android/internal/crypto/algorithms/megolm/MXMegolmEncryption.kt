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

package im.vector.matrix.android.internal.crypto.algorithms.megolm

import android.text.TextUtils
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.internal.crypto.CryptoAsyncHelper
import im.vector.matrix.android.internal.crypto.DeviceListManager
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import im.vector.matrix.android.internal.crypto.MXOlmDevice
import im.vector.matrix.android.internal.crypto.actions.EnsureOlmSessionsForDevicesAction
import im.vector.matrix.android.internal.crypto.actions.MessageEncrypter
import im.vector.matrix.android.internal.crypto.algorithms.IMXEncrypting
import im.vector.matrix.android.internal.crypto.keysbackup.KeysBackup
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXOlmSessionResult
import im.vector.matrix.android.internal.crypto.model.MXQueuedEncryption
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.repository.WarnOnUnknownDeviceRepository
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.tasks.SendToDeviceTask
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.convertToUTF8
import timber.log.Timber
import java.util.*

internal class MXMegolmEncryption(
        // The id of the room we will be sending to.
        private var mRoomId: String,

        private val olmDevice: MXOlmDevice,
        private val mKeysBackup: KeysBackup,
        private val mCryptoStore: IMXCryptoStore,
        private val mDeviceListManager: DeviceListManager,
        private val mEnsureOlmSessionsForDevicesAction: EnsureOlmSessionsForDevicesAction,
        private val mCredentials: Credentials,
        private val mSendToDeviceTask: SendToDeviceTask,
        private val mTaskExecutor: TaskExecutor,
        private val mMessageEncrypter: MessageEncrypter,
        private val mWarnOnUnknownDevicesRepository: WarnOnUnknownDeviceRepository
        ) : IMXEncrypting {


    // OutboundSessionInfo. Null if we haven't yet started setting one up. Note
    // that even if this is non-null, it may not be ready for use (in which
    // case outboundSession.shareOperation will be non-null.)
    private var mOutboundSession: MXOutboundSessionInfo? = null

    // true when there is an HTTP operation in progress
    private var mShareOperationIsProgress: Boolean = false

    private val mPendingEncryptions = ArrayList<MXQueuedEncryption>()

    // Default rotation periods
    // TODO: Make it configurable via parameters
    // Session rotation periods
    private var mSessionRotationPeriodMsgs: Int = 100
    private var mSessionRotationPeriodMs: Int = 7 * 24 * 3600 * 1000

    /**
     * @return a snapshot of the pending encryptions
     */
    private val pendingEncryptions: List<MXQueuedEncryption>
        get() {
            val list = ArrayList<MXQueuedEncryption>()

            synchronized(mPendingEncryptions) {
                list.addAll(mPendingEncryptions)
            }

            return list
        }

    override fun encryptEventContent(eventContent: Content,
                                     eventType: String,
                                     userIds: List<String>,
                                     callback: MatrixCallback<Content>) {
        // Queue the encryption request
        // It will be processed when everything is set up
        val queuedEncryption = MXQueuedEncryption()

        queuedEncryption.mEventContent = eventContent
        queuedEncryption.mEventType = eventType
        queuedEncryption.mApiCallback = callback

        synchronized(mPendingEncryptions) {
            mPendingEncryptions.add(queuedEncryption)
        }

        val t0 = System.currentTimeMillis()
        Timber.d("## encryptEventContent () starts")

        getDevicesInRoom(userIds, object : MatrixCallback<MXUsersDevicesMap<MXDeviceInfo>> {

            /**
             * A network error has been received while encrypting
             * @param failure the exception
             */
            private fun dispatchFailure(failure: Throwable) {
                Timber.e(failure, "## encryptEventContent() : failure")
                val queuedEncryptions = pendingEncryptions

                for (queuedEncryption in queuedEncryptions) {
                    queuedEncryption.mApiCallback?.onFailure(failure)
                }

                synchronized(mPendingEncryptions) {
                    mPendingEncryptions.removeAll(queuedEncryptions)
                }
            }

            override fun onSuccess(devicesInRoom: MXUsersDevicesMap<MXDeviceInfo>) {
                ensureOutboundSession(devicesInRoom, object : MatrixCallback<MXOutboundSessionInfo> {
                    override fun onSuccess(data: MXOutboundSessionInfo) {
                        Timber.d("## encryptEventContent () processPendingEncryptions after " + (System.currentTimeMillis() - t0) + "ms")
                        processPendingEncryptions(data)
                    }

                    override fun onFailure(failure: Throwable) {
                        dispatchFailure(failure)
                    }
                })
            }

            override fun onFailure(failure: Throwable) {
                dispatchFailure(failure)
            }
        })
    }

    /**
     * Prepare a new session.
     *
     * @return the session description
     */
    private fun prepareNewSessionInRoom(): MXOutboundSessionInfo {
        val sessionId = olmDevice.createOutboundGroupSession()

        val keysClaimedMap = HashMap<String, String>()
        keysClaimedMap["ed25519"] = olmDevice.deviceEd25519Key!!

        olmDevice.addInboundGroupSession(sessionId!!, olmDevice.getSessionKey(sessionId)!!, mRoomId, olmDevice.deviceCurve25519Key!!,
                ArrayList(), keysClaimedMap, false)

        mKeysBackup.maybeBackupKeys()

        return MXOutboundSessionInfo(sessionId)
    }

    /**
     * Ensure the outbound session
     *
     * @param devicesInRoom the devices list
     * @param callback      the asynchronous callback.
     */
    private fun ensureOutboundSession(devicesInRoom: MXUsersDevicesMap<MXDeviceInfo>, callback: MatrixCallback<MXOutboundSessionInfo>?) {
        var session = mOutboundSession

        if (null == session
                // Need to make a brand new session?
                || session.needsRotation(mSessionRotationPeriodMsgs, mSessionRotationPeriodMs)
                // Determine if we have shared with anyone we shouldn't have
                || session.sharedWithTooManyDevices(devicesInRoom)) {
            session = prepareNewSessionInRoom()
            mOutboundSession = session
        }

        if (mShareOperationIsProgress) {
            Timber.d("## ensureOutboundSessionInRoom() : already in progress")
            // Key share already in progress
            return
        }

        val fSession = session

        val shareMap = HashMap<String, MutableList<MXDeviceInfo>>()/* userId */

        val userIds = devicesInRoom.userIds

        for (userId in userIds) {
            val deviceIds = devicesInRoom.getUserDeviceIds(userId)

            for (deviceId in deviceIds!!) {
                val deviceInfo = devicesInRoom.getObject(deviceId, userId)

                if (null == fSession.mSharedWithDevices.getObject(deviceId, userId)) {
                    if (!shareMap.containsKey(userId)) {
                        shareMap[userId] = ArrayList()
                    }

                    shareMap[userId]!!.add(deviceInfo)
                }
            }
        }

        shareKey(fSession, shareMap, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                mShareOperationIsProgress = false
                callback?.onSuccess(fSession)
            }

            override fun onFailure(failure: Throwable) {
                Timber.e("## ensureOutboundSessionInRoom() : shareKey onFailure")

                callback?.onFailure(failure)
                mShareOperationIsProgress = false
            }
        })

    }

    /**
     * Share the device key to a list of users
     *
     * @param session        the session info
     * @param devicesByUsers the devices map
     * @param callback       the asynchronous callback
     */
    private fun shareKey(session: MXOutboundSessionInfo,
                         devicesByUsers: MutableMap<String, MutableList<MXDeviceInfo>>,
                         callback: MatrixCallback<Unit>?) {
        // nothing to send, the task is done
        if (0 == devicesByUsers.size) {
            Timber.d("## shareKey() : nothing more to do")

            if (null != callback) {
                CryptoAsyncHelper.getUiHandler().post { callback.onSuccess(Unit) }
            }

            return
        }

        // reduce the map size to avoid request timeout when there are too many devices (Users size  * devices per user)
        val subMap = HashMap<String, List<MXDeviceInfo>>()

        val userIds = ArrayList<String>()
        var devicesCount = 0

        for (userId in devicesByUsers.keys) {
            val devicesList = devicesByUsers[userId]

            userIds.add(userId)
            subMap[userId] = devicesList!!

            devicesCount += devicesList.size

            if (devicesCount > 100) {
                break
            }
        }

        Timber.d("## shareKey() ; userId $userIds")
        shareUserDevicesKey(session, subMap, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                for (userId in userIds) {
                    devicesByUsers.remove(userId)
                }
                shareKey(session, devicesByUsers, callback)
            }

            override fun onFailure(failure: Throwable) {
                Timber.e(failure, "## shareKey() ; userIds " + userIds + " failed")
                callback?.onFailure(failure)
            }
        })
    }

    /**
     * Share the device keys of a an user
     *
     * @param session       the session info
     * @param devicesByUser the devices map
     * @param callback      the asynchronous callback
     */
    private fun shareUserDevicesKey(session: MXOutboundSessionInfo,
                                    devicesByUser: Map<String, List<MXDeviceInfo>>,
                                    callback: MatrixCallback<Unit>?) {
        val sessionKey = olmDevice.getSessionKey(session.mSessionId)
        val chainIndex = olmDevice.getMessageIndex(session.mSessionId)

        val submap = HashMap<String, Any>()
        submap["algorithm"] = MXCRYPTO_ALGORITHM_MEGOLM
        submap["room_id"] = mRoomId
        submap["session_id"] = session.mSessionId
        submap["session_key"] = sessionKey!!
        submap["chain_index"] = chainIndex

        val payload = HashMap<String, Any>()
        payload["type"] = EventType.ROOM_KEY
        payload["content"] = submap

        val t0 = System.currentTimeMillis()
        Timber.d("## shareUserDevicesKey() : starts")

        mEnsureOlmSessionsForDevicesAction.handle(devicesByUser, object : MatrixCallback<MXUsersDevicesMap<MXOlmSessionResult>> {
            override fun onSuccess(data: MXUsersDevicesMap<MXOlmSessionResult>) {
                Timber.d("## shareUserDevicesKey() : ensureOlmSessionsForDevices succeeds after "
                        + (System.currentTimeMillis() - t0) + " ms")
                val contentMap = MXUsersDevicesMap<Any>()

                var haveTargets = false
                val userIds = data.userIds

                for (userId in userIds) {
                    val devicesToShareWith = devicesByUser[userId]

                    for ((deviceID) in devicesToShareWith!!) {

                        val sessionResult = data.getObject(deviceID, userId)

                        if (null == sessionResult || null == sessionResult.mSessionId) {
                            // no session with this device, probably because there
                            // were no one-time keys.
                            //
                            // we could send them a to_device message anyway, as a
                            // signal that they have missed out on the key sharing
                            // message because of the lack of keys, but there's not
                            // much point in that really; it will mostly serve to clog
                            // up to_device inboxes.
                            //
                            // ensureOlmSessionsForUsers has already done the logging,
                            // so just skip it.
                            continue
                        }

                        Timber.d("## shareUserDevicesKey() : Sharing keys with device $userId:$deviceID")
                        //noinspection ArraysAsListWithZeroOrOneArgument,ArraysAsListWithZeroOrOneArgument
                        contentMap.setObject(mMessageEncrypter.encryptMessage(payload, Arrays.asList(sessionResult.mDevice)), userId, deviceID)
                        haveTargets = true
                    }
                }

                if (haveTargets) {
                    val t0 = System.currentTimeMillis()
                    Timber.d("## shareUserDevicesKey() : has target")

                    mSendToDeviceTask.configureWith(SendToDeviceTask.Params(EventType.ENCRYPTED, contentMap))
                            .dispatchTo(object : MatrixCallback<Unit> {
                                override fun onSuccess(data: Unit) {
                                    Timber.d("## shareUserDevicesKey() : sendToDevice succeeds after "
                                            + (System.currentTimeMillis() - t0) + " ms")

                                    // Add the devices we have shared with to session.sharedWithDevices.
                                    // we deliberately iterate over devicesByUser (ie, the devices we
                                    // attempted to share with) rather than the contentMap (those we did
                                    // share with), because we don't want to try to claim a one-time-key
                                    // for dead devices on every message.
                                    for (userId in devicesByUser.keys) {
                                        val devicesToShareWith = devicesByUser[userId]

                                        for ((deviceId) in devicesToShareWith!!) {
                                            session.mSharedWithDevices.setObject(chainIndex, userId, deviceId)
                                        }
                                    }

                                    CryptoAsyncHelper.getUiHandler().post {
                                        callback?.onSuccess(Unit)
                                    }
                                }

                                override fun onFailure(failure: Throwable) {
                                    Timber.e(failure, "## shareUserDevicesKey() : sendToDevice")

                                    callback?.onFailure(failure)
                                }
                            })
                            .executeBy(mTaskExecutor)
                } else {
                    Timber.d("## shareUserDevicesKey() : no need to sharekey")

                    if (null != callback) {
                        CryptoAsyncHelper.getUiHandler().post { callback.onSuccess(Unit) }
                    }
                }
            }

            override fun onFailure(failure: Throwable) {
                Timber.e(failure, "## shareUserDevicesKey() : ensureOlmSessionsForDevices failed")

                callback?.onFailure(failure)
            }
        })
    }

    /**
     * process the pending encryptions
     */
    private fun processPendingEncryptions(session: MXOutboundSessionInfo?) {
        if (null != session) {
            val queuedEncryptions = pendingEncryptions

            // Everything is in place, encrypt all pending events
            for (queuedEncryption in queuedEncryptions) {
                val payloadJson = HashMap<String, Any>()

                payloadJson["room_id"] = mRoomId
                payloadJson["type"] = queuedEncryption.mEventType!!
                payloadJson["content"] = queuedEncryption.mEventContent!!

                // Get canonical Json from
                val content = payloadJson.toContent()!!

                val payloadString = convertToUTF8(MoshiProvider.getCanonicalJson(Map::class.java, content))
                val ciphertext = olmDevice.encryptGroupMessage(session.mSessionId, payloadString!!)

                val map = HashMap<String, Any>()
                map["algorithm"] = MXCRYPTO_ALGORITHM_MEGOLM
                map["sender_key"] = olmDevice.deviceCurve25519Key!!
                map["ciphertext"] = ciphertext!!
                map["session_id"] = session.mSessionId

                // Include our device ID so that recipients can send us a
                // m.new_device message if they don't have our session key.
                map["device_id"] = mCredentials.deviceId!!

                CryptoAsyncHelper.getUiHandler().post { queuedEncryption.mApiCallback?.onSuccess(map.toContent()!!) }

                session.mUseCount++
            }

            synchronized(mPendingEncryptions) {
                mPendingEncryptions.removeAll(queuedEncryptions)
            }
        }
    }

    /**
     * Get the list of devices which can encrypt data to.
     * This method must be called in getDecryptingThreadHandler() thread.
     *
     * @param userIds  the user ids whose devices must be checked.
     * @param callback the asynchronous callback
     */
    private fun getDevicesInRoom(userIds: List<String>, callback: MatrixCallback<MXUsersDevicesMap<MXDeviceInfo>>) {
        // We are happy to use a cached version here: we assume that if we already
        // have a list of the user's devices, then we already share an e2e room
        // with them, which means that they will have announced any new devices via
        // an m.new_device.
        mDeviceListManager.downloadKeys(userIds, false, object : MatrixCallback<MXUsersDevicesMap<MXDeviceInfo>> {
            override fun onSuccess(data: MXUsersDevicesMap<MXDeviceInfo>) {
                val encryptToVerifiedDevicesOnly = mCryptoStore.getGlobalBlacklistUnverifiedDevices()
                        || mCryptoStore.getRoomsListBlacklistUnverifiedDevices().contains(mRoomId)

                val devicesInRoom = MXUsersDevicesMap<MXDeviceInfo>()
                val unknownDevices = MXUsersDevicesMap<MXDeviceInfo>()

                for (userId in data.userIds) {
                    val deviceIds = data.getUserDeviceIds(userId)

                    for (deviceId in deviceIds!!) {
                        val deviceInfo = data.getObject(deviceId, userId)

                        if (mWarnOnUnknownDevicesRepository.warnOnUnknownDevices() && deviceInfo!!.isUnknown) {
                            // The device is not yet known by the user
                            unknownDevices.setObject(deviceInfo, userId, deviceId)
                            continue
                        }

                        if (deviceInfo!!.isBlocked) {
                            // Remove any blocked devices
                            continue
                        }

                        if (!deviceInfo.isVerified && encryptToVerifiedDevicesOnly) {
                            continue
                        }

                        if (TextUtils.equals(deviceInfo.identityKey(), olmDevice.deviceCurve25519Key)) {
                            // Don't bother sending to ourself
                            continue
                        }

                        devicesInRoom.setObject(deviceInfo, userId, deviceId)
                    }
                }

                CryptoAsyncHelper.getUiHandler().post {
                    // Check if any of these devices are not yet known to the user.
                    // if so, warn the user so they can verify or ignore.
                    if (0 != unknownDevices.map.size) {
                        callback.onFailure(Failure.CryptoError(MXCryptoError(MXCryptoError.UNKNOWN_DEVICES_CODE,
                                MXCryptoError.UNABLE_TO_ENCRYPT, MXCryptoError.UNKNOWN_DEVICES_REASON, unknownDevices)))
                    } else {
                        callback.onSuccess(devicesInRoom)
                    }
                }
            }

            override fun onFailure(failure: Throwable) {
                callback.onFailure(failure)
            }
        })
    }
}
