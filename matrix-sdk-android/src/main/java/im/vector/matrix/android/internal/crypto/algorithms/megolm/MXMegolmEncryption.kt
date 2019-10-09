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
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.crypto.DeviceListManager
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import im.vector.matrix.android.internal.crypto.MXOlmDevice
import im.vector.matrix.android.internal.crypto.actions.EnsureOlmSessionsForDevicesAction
import im.vector.matrix.android.internal.crypto.actions.MessageEncrypter
import im.vector.matrix.android.internal.crypto.algorithms.IMXEncrypting
import im.vector.matrix.android.internal.crypto.keysbackup.KeysBackup
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.repository.WarnOnUnknownDeviceRepository
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.tasks.SendToDeviceTask
import im.vector.matrix.android.internal.util.JsonCanonicalizer
import im.vector.matrix.android.internal.util.convertToUTF8
import timber.log.Timber
import java.util.*

internal class MXMegolmEncryption(
        // The id of the room we will be sending to.
        private var roomId: String,
        private val olmDevice: MXOlmDevice,
        private val keysBackup: KeysBackup,
        private val cryptoStore: IMXCryptoStore,
        private val deviceListManager: DeviceListManager,
        private val ensureOlmSessionsForDevicesAction: EnsureOlmSessionsForDevicesAction,
        private val credentials: Credentials,
        private val sendToDeviceTask: SendToDeviceTask,
        private val messageEncrypter: MessageEncrypter,
        private val warnOnUnknownDevicesRepository: WarnOnUnknownDeviceRepository
) : IMXEncrypting {

    // OutboundSessionInfo. Null if we haven't yet started setting one up. Note
    // that even if this is non-null, it may not be ready for use (in which
    // case outboundSession.shareOperation will be non-null.)
    private var outboundSession: MXOutboundSessionInfo? = null

    // Default rotation periods
    // TODO: Make it configurable via parameters
    // Session rotation periods
    private var sessionRotationPeriodMsgs: Int = 100
    private var sessionRotationPeriodMs: Int = 7 * 24 * 3600 * 1000

    override suspend fun encryptEventContent(eventContent: Content,
                                             eventType: String,
                                             userIds: List<String>): Content {
        val devices = getDevicesInRoom(userIds)
        val outboundSession = ensureOutboundSession(devices)
        return encryptContent(outboundSession, eventType, eventContent)
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

        olmDevice.addInboundGroupSession(sessionId!!, olmDevice.getSessionKey(sessionId)!!, roomId, olmDevice.deviceCurve25519Key!!,
                ArrayList(), keysClaimedMap, false)

        keysBackup.maybeBackupKeys()

        return MXOutboundSessionInfo(sessionId)
    }

    /**
     * Ensure the outbound session
     *
     * @param devicesInRoom the devices list
     */
    private suspend fun ensureOutboundSession(devicesInRoom: MXUsersDevicesMap<MXDeviceInfo>): MXOutboundSessionInfo {
        var session = outboundSession
        if (session == null
                // Need to make a brand new session?
                || session.needsRotation(sessionRotationPeriodMsgs, sessionRotationPeriodMs)
                // Determine if we have shared with anyone we shouldn't have
                || session.sharedWithTooManyDevices(devicesInRoom)) {
            session = prepareNewSessionInRoom()
            outboundSession = session
        }
        val safeSession = session
        val shareMap = HashMap<String, MutableList<MXDeviceInfo>>()/* userId */
        val userIds = devicesInRoom.userIds
        for (userId in userIds) {
            val deviceIds = devicesInRoom.getUserDeviceIds(userId)
            for (deviceId in deviceIds!!) {
                val deviceInfo = devicesInRoom.getObject(userId, deviceId)
                if (deviceInfo != null && null == safeSession.sharedWithDevices.getObject(userId, deviceId)) {
                    if (!shareMap.containsKey(userId)) {
                        shareMap[userId] = ArrayList()
                    }
                    shareMap[userId]!!.add(deviceInfo)
                }
            }
        }
        shareKey(safeSession, shareMap)
        return safeSession
    }

    /**
     * Share the device key to a list of users
     *
     * @param session        the session info
     * @param devicesByUsers the devices map
     */
    private suspend fun shareKey(session: MXOutboundSessionInfo,
                                 devicesByUsers: Map<String, List<MXDeviceInfo>>) {
        // nothing to send, the task is done
        if (devicesByUsers.isEmpty()) {
            Timber.v("## shareKey() : nothing more to do")
            return
        }
        // reduce the map size to avoid request timeout when there are too many devices (Users size  * devices per user)
        val subMap = HashMap<String, List<MXDeviceInfo>>()
        val userIds = ArrayList<String>()
        var devicesCount = 0
        for (userId in devicesByUsers.keys) {
            devicesByUsers[userId]?.let {
                userIds.add(userId)
                subMap[userId] = it
                devicesCount += it.size
            }
            if (devicesCount > 100) {
                break
            }
        }
        Timber.v("## shareKey() ; userId $userIds")
        shareUserDevicesKey(session, subMap)
        val remainingDevices = devicesByUsers.filterKeys { userIds.contains(it).not() }
        shareKey(session, remainingDevices)
    }

    /**
     * Share the device keys of a an user
     *
     * @param session       the session info
     * @param devicesByUser the devices map
     * @param callback      the asynchronous callback
     */
    private suspend fun shareUserDevicesKey(session: MXOutboundSessionInfo,
                                            devicesByUser: Map<String, List<MXDeviceInfo>>) {
        val sessionKey = olmDevice.getSessionKey(session.sessionId)
        val chainIndex = olmDevice.getMessageIndex(session.sessionId)

        val submap = HashMap<String, Any>()
        submap["algorithm"] = MXCRYPTO_ALGORITHM_MEGOLM
        submap["room_id"] = roomId
        submap["session_id"] = session.sessionId
        submap["session_key"] = sessionKey!!
        submap["chain_index"] = chainIndex

        val payload = HashMap<String, Any>()
        payload["type"] = EventType.ROOM_KEY
        payload["content"] = submap

        var t0 = System.currentTimeMillis()
        Timber.v("## shareUserDevicesKey() : starts")

        val results = ensureOlmSessionsForDevicesAction.handle(devicesByUser)
        Timber.v("## shareUserDevicesKey() : ensureOlmSessionsForDevices succeeds after "
                + (System.currentTimeMillis() - t0) + " ms")
        val contentMap = MXUsersDevicesMap<Any>()
        var haveTargets = false
        val userIds = results.userIds
        for (userId in userIds) {
            val devicesToShareWith = devicesByUser[userId]
            for ((deviceID) in devicesToShareWith!!) {
                val sessionResult = results.getObject(userId, deviceID)
                if (sessionResult?.sessionId == null) {
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
                Timber.v("## shareUserDevicesKey() : Sharing keys with device $userId:$deviceID")
                //noinspection ArraysAsListWithZeroOrOneArgument,ArraysAsListWithZeroOrOneArgument
                contentMap.setObject(userId, deviceID, messageEncrypter.encryptMessage(payload, Arrays.asList(sessionResult.deviceInfo)))
                haveTargets = true
            }
        }
        if (haveTargets) {
            t0 = System.currentTimeMillis()
            Timber.v("## shareUserDevicesKey() : has target")
            val sendToDeviceParams = SendToDeviceTask.Params(EventType.ENCRYPTED, contentMap)
            sendToDeviceTask.execute(sendToDeviceParams)
            Timber.v("## shareUserDevicesKey() : sendToDevice succeeds after "
                    + (System.currentTimeMillis() - t0) + " ms")

            // Add the devices we have shared with to session.sharedWithDevices.
            // we deliberately iterate over devicesByUser (ie, the devices we
            // attempted to share with) rather than the contentMap (those we did
            // share with), because we don't want to try to claim a one-time-key
            // for dead devices on every message.
            for (userId in devicesByUser.keys) {
                val devicesToShareWith = devicesByUser[userId]
                for ((deviceId) in devicesToShareWith!!) {
                    session.sharedWithDevices.setObject(userId, deviceId, chainIndex)
                }
            }
        } else {
            Timber.v("## shareUserDevicesKey() : no need to sharekey")
        }
    }

    /**
     * process the pending encryptions
     */
    private fun encryptContent(session: MXOutboundSessionInfo, eventType: String, eventContent: Content): Content {
        // Everything is in place, encrypt all pending events
        val payloadJson = HashMap<String, Any>()
        payloadJson["room_id"] = roomId
        payloadJson["type"] = eventType
        payloadJson["content"] = eventContent

        // Get canonical Json from

        val payloadString = convertToUTF8(JsonCanonicalizer.getCanonicalJson(Map::class.java, payloadJson))
        val ciphertext = olmDevice.encryptGroupMessage(session.sessionId, payloadString)

        val map = HashMap<String, Any>()
        map["algorithm"] = MXCRYPTO_ALGORITHM_MEGOLM
        map["sender_key"] = olmDevice.deviceCurve25519Key!!
        map["ciphertext"] = ciphertext!!
        map["session_id"] = session.sessionId

        // Include our device ID so that recipients can send us a
        // m.new_device message if they don't have our session key.
        map["device_id"] = credentials.deviceId!!
        session.useCount++
        return map
    }

    /**
     * Get the list of devices which can encrypt data to.
     * This method must be called in getDecryptingThreadHandler() thread.
     *
     * @param userIds  the user ids whose devices must be checked.
     * @param callback the asynchronous callback
     */
    private suspend fun getDevicesInRoom(userIds: List<String>): MXUsersDevicesMap<MXDeviceInfo> {
        // We are happy to use a cached version here: we assume that if we already
        // have a list of the user's devices, then we already share an e2e room
        // with them, which means that they will have announced any new devices via
        // an m.new_device.
        val keys = deviceListManager.downloadKeys(userIds, false)
        val encryptToVerifiedDevicesOnly = cryptoStore.getGlobalBlacklistUnverifiedDevices()
                || cryptoStore.getRoomsListBlacklistUnverifiedDevices().contains(roomId)

        val devicesInRoom = MXUsersDevicesMap<MXDeviceInfo>()
        val unknownDevices = MXUsersDevicesMap<MXDeviceInfo>()

        for (userId in keys.userIds) {
            val deviceIds = keys.getUserDeviceIds(userId) ?: continue
            for (deviceId in deviceIds) {
                val deviceInfo = keys.getObject(userId, deviceId) ?: continue
                if (warnOnUnknownDevicesRepository.warnOnUnknownDevices() && deviceInfo.isUnknown) {
                    // The device is not yet known by the user
                    unknownDevices.setObject(userId, deviceId, deviceInfo)
                    continue
                }
                if (deviceInfo.isBlocked) {
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
                devicesInRoom.setObject(userId, deviceId, deviceInfo)
            }
        }
        if (unknownDevices.isEmpty) {
            return devicesInRoom
        } else {
            throw MXCryptoError.UnknownDevice(unknownDevices)
        }
    }
}
