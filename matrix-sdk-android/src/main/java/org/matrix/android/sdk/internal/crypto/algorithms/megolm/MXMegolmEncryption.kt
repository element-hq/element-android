/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.algorithms.megolm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.crypto.model.forEach
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.RoomKeyWithHeldContent
import org.matrix.android.sdk.api.session.events.model.content.WithHeldCode
import org.matrix.android.sdk.internal.crypto.DeviceListManager
import org.matrix.android.sdk.internal.crypto.MXOlmDevice
import org.matrix.android.sdk.internal.crypto.actions.EnsureOlmSessionsForDevicesAction
import org.matrix.android.sdk.internal.crypto.actions.MessageEncrypter
import org.matrix.android.sdk.internal.crypto.algorithms.IMXEncrypting
import org.matrix.android.sdk.internal.crypto.algorithms.IMXGroupEncryption
import org.matrix.android.sdk.internal.crypto.keysbackup.DefaultKeysBackupService
import org.matrix.android.sdk.internal.crypto.model.toDebugCount
import org.matrix.android.sdk.internal.crypto.model.toDebugString
import org.matrix.android.sdk.internal.crypto.repository.WarnOnUnknownDeviceRepository
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.android.sdk.internal.util.convertToUTF8
import timber.log.Timber

private val loggerTag = LoggerTag("MXMegolmEncryption", LoggerTag.CRYPTO)

internal class MXMegolmEncryption(
        // The id of the room we will be sending to.
        private val roomId: String,
        private val olmDevice: MXOlmDevice,
        private val defaultKeysBackupService: DefaultKeysBackupService,
        private val cryptoStore: IMXCryptoStore,
        private val deviceListManager: DeviceListManager,
        private val ensureOlmSessionsForDevicesAction: EnsureOlmSessionsForDevicesAction,
        private val myUserId: String,
        private val myDeviceId: String,
        private val sendToDeviceTask: SendToDeviceTask,
        private val messageEncrypter: MessageEncrypter,
        private val warnOnUnknownDevicesRepository: WarnOnUnknownDeviceRepository,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoCoroutineScope: CoroutineScope
) : IMXEncrypting, IMXGroupEncryption {

    // OutboundSessionInfo. Null if we haven't yet started setting one up. Note
    // that even if this is non-null, it may not be ready for use (in which
    // case outboundSession.shareOperation will be non-null.)
    private var outboundSession: MXOutboundSessionInfo? = null

    init {
        // restore existing outbound session if any
        outboundSession = olmDevice.restoreOutboundGroupSessionForRoom(roomId)
    }

    // Default rotation periods
    // TODO: Make it configurable via parameters
    // Session rotation periods
    private var sessionRotationPeriodMsgs: Int = 100
    private var sessionRotationPeriodMs: Int = 7 * 24 * 3600 * 1000

    override suspend fun encryptEventContent(eventContent: Content,
                                             eventType: String,
                                             userIds: List<String>): Content {
        val ts = System.currentTimeMillis()
        Timber.tag(loggerTag.value).v("encryptEventContent : getDevicesInRoom")
        val devices = getDevicesInRoom(userIds)
        Timber.tag(loggerTag.value).d("encrypt event in room=$roomId - devices count in room ${devices.allowedDevices.toDebugCount()}")
        Timber.tag(loggerTag.value).v("encryptEventContent ${System.currentTimeMillis() - ts}: getDevicesInRoom ${devices.allowedDevices.toDebugString()}")
        val outboundSession = ensureOutboundSession(devices.allowedDevices)

        return encryptContent(outboundSession, eventType, eventContent)
                .also {
                    notifyWithheldForSession(devices.withHeldDevices, outboundSession)
                    // annoyingly we have to serialize again the saved outbound session to store message index :/
                    // if not we would see duplicate message index errors
                    olmDevice.storeOutboundGroupSessionForRoom(roomId, outboundSession.sessionId)
                    Timber.tag(loggerTag.value).d("encrypt event in room=$roomId Finished in ${System.currentTimeMillis() - ts} millis")
                }
    }

    private fun notifyWithheldForSession(devices: MXUsersDevicesMap<WithHeldCode>, outboundSession: MXOutboundSessionInfo) {
        // offload to computation thread
        cryptoCoroutineScope.launch(coroutineDispatchers.computation) {
            mutableListOf<Pair<UserDevice, WithHeldCode>>().apply {
                devices.forEach { userId, deviceId, withheldCode ->
                    this.add(UserDevice(userId, deviceId) to withheldCode)
                }
            }.groupBy(
                    { it.second },
                    { it.first }
            ).forEach { (code, targets) ->
                notifyKeyWithHeld(targets, outboundSession.sessionId, olmDevice.deviceCurve25519Key, code)
            }
        }
    }

    override fun discardSessionKey() {
        outboundSession = null
        olmDevice.discardOutboundGroupSessionForRoom(roomId)
    }

    override suspend fun preshareKey(userIds: List<String>) {
        val ts = System.currentTimeMillis()
        Timber.tag(loggerTag.value).d("preshareKey started in $roomId ...")
        val devices = getDevicesInRoom(userIds)
        val outboundSession = ensureOutboundSession(devices.allowedDevices)

        notifyWithheldForSession(devices.withHeldDevices, outboundSession)

        Timber.tag(loggerTag.value).d("preshareKey in $roomId done in  ${System.currentTimeMillis() - ts} millis")
    }

    /**
     * Prepare a new session.
     *
     * @return the session description
     */
    private fun prepareNewSessionInRoom(): MXOutboundSessionInfo {
        Timber.tag(loggerTag.value).v("prepareNewSessionInRoom() ")
        val sessionId = olmDevice.createOutboundGroupSessionForRoom(roomId)

        val keysClaimedMap = mapOf(
                "ed25519" to olmDevice.deviceEd25519Key!!
        )

        olmDevice.addInboundGroupSession(sessionId!!, olmDevice.getSessionKey(sessionId)!!, roomId, olmDevice.deviceCurve25519Key!!,
                emptyList(), keysClaimedMap, false)

        defaultKeysBackupService.maybeBackupKeys()

        return MXOutboundSessionInfo(sessionId, SharedWithHelper(roomId, sessionId, cryptoStore))
    }

    /**
     * Ensure the outbound session
     *
     * @param devicesInRoom the devices list
     */
    private suspend fun ensureOutboundSession(devicesInRoom: MXUsersDevicesMap<CryptoDeviceInfo>): MXOutboundSessionInfo {
        Timber.tag(loggerTag.value).v("ensureOutboundSession roomId:$roomId")
        var session = outboundSession
        if (session == null ||
                // Need to make a brand new session?
                session.needsRotation(sessionRotationPeriodMsgs, sessionRotationPeriodMs) ||
                // Determine if we have shared with anyone we shouldn't have
                session.sharedWithTooManyDevices(devicesInRoom)) {
            Timber.tag(loggerTag.value).d("roomId:$roomId Starting new megolm session because we need to rotate.")
            session = prepareNewSessionInRoom()
            outboundSession = session
        }
        val safeSession = session
        val shareMap = HashMap<String, MutableList<CryptoDeviceInfo>>()/* userId */
        val userIds = devicesInRoom.userIds
        for (userId in userIds) {
            val deviceIds = devicesInRoom.getUserDeviceIds(userId)
            for (deviceId in deviceIds!!) {
                val deviceInfo = devicesInRoom.getObject(userId, deviceId)
                if (deviceInfo != null && !cryptoStore.getSharedSessionInfo(roomId, safeSession.sessionId, deviceInfo).found) {
                    val devices = shareMap.getOrPut(userId) { ArrayList() }
                    devices.add(deviceInfo)
                }
            }
        }
        val devicesCount = shareMap.entries.fold(0) { acc, new -> acc + new.value.size }
        Timber.tag(loggerTag.value).d("roomId:$roomId found $devicesCount devices without megolm session(${session.sessionId})")
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
                                 devicesByUsers: Map<String, List<CryptoDeviceInfo>>) {
        // nothing to send, the task is done
        if (devicesByUsers.isEmpty()) {
            Timber.tag(loggerTag.value).v("shareKey() : nothing more to do")
            return
        }
        // reduce the map size to avoid request timeout when there are too many devices (Users size  * devices per user)
        val subMap = HashMap<String, List<CryptoDeviceInfo>>()
        var devicesCount = 0
        for ((userId, devices) in devicesByUsers) {
            subMap[userId] = devices
            devicesCount += devices.size
            if (devicesCount > 100) {
                break
            }
        }
        Timber.tag(loggerTag.value).v("shareKey() ; sessionId<${session.sessionId}> userId ${subMap.keys}")
        shareUserDevicesKey(session, subMap)
        val remainingDevices = devicesByUsers - subMap.keys
        shareKey(session, remainingDevices)
    }

    /**
     * Share the device keys of a an user
     *
     * @param session       the session info
     * @param devicesByUser the devices map
     */
    private suspend fun shareUserDevicesKey(session: MXOutboundSessionInfo,
                                            devicesByUser: Map<String, List<CryptoDeviceInfo>>) {
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
        Timber.tag(loggerTag.value).v("shareUserDevicesKey() : starts")

        val results = ensureOlmSessionsForDevicesAction.handle(devicesByUser)
        Timber.tag(loggerTag.value).v(
                """shareUserDevicesKey(): ensureOlmSessionsForDevices succeeds after ${System.currentTimeMillis() - t0} ms"""
                        .trimMargin()
        )
        val contentMap = MXUsersDevicesMap<Any>()
        var haveTargets = false
        val userIds = results.userIds
        val noOlmToNotify = mutableListOf<UserDevice>()
        for (userId in userIds) {
            val devicesToShareWith = devicesByUser[userId]
            for ((deviceID) in devicesToShareWith!!) {
                val sessionResult = results.getObject(userId, deviceID)
                if (sessionResult?.sessionId == null) {
                    // no session with this device, probably because there
                    // were no one-time keys.

                    // MSC 2399
                    // send withheld m.no_olm: an olm session could not be established.
                    // This may happen, for example, if the sender was unable to obtain a one-time key from the recipient.
                    Timber.tag(loggerTag.value).v("shareUserDevicesKey() : No Olm Session for $userId:$deviceID mark for withheld")
                    noOlmToNotify.add(UserDevice(userId, deviceID))
                    continue
                }
                Timber.tag(loggerTag.value).v("shareUserDevicesKey() : Add to share keys contentMap for $userId:$deviceID")
                contentMap.setObject(userId, deviceID, messageEncrypter.encryptMessage(payload, listOf(sessionResult.deviceInfo)))
                haveTargets = true
            }
        }

        // Add the devices we have shared with to session.sharedWithDevices.
        // we deliberately iterate over devicesByUser (ie, the devices we
        // attempted to share with) rather than the contentMap (those we did
        // share with), because we don't want to try to claim a one-time-key
        // for dead devices on every message.
        val gossipingEventBuffer = arrayListOf<Event>()
        for ((userId, devicesToShareWith) in devicesByUser) {
            for (deviceInfo in devicesToShareWith) {
                session.sharedWithHelper.markedSessionAsShared(deviceInfo, chainIndex)
                gossipingEventBuffer.add(
                        Event(
                                type = EventType.ROOM_KEY,
                                senderId = myUserId,
                                content = submap.apply {
                                    this["session_key"] = ""
                                    // we add a fake key for trail
                                    this["_dest"] = "$userId|${deviceInfo.deviceId}"
                                }
                        ))
            }
        }

        cryptoStore.saveGossipingEvents(gossipingEventBuffer)

        if (haveTargets) {
            t0 = System.currentTimeMillis()
            Timber.tag(loggerTag.value).i("shareUserDevicesKey() ${session.sessionId} : has target")
            Timber.tag(loggerTag.value).d("sending to device room key for ${session.sessionId} to ${contentMap.toDebugString()}")
            val sendToDeviceParams = SendToDeviceTask.Params(EventType.ENCRYPTED, contentMap)
            try {
                withContext(coroutineDispatchers.io) {
                    sendToDeviceTask.execute(sendToDeviceParams)
                }
                Timber.tag(loggerTag.value).i("shareUserDevicesKey() : sendToDevice succeeds after ${System.currentTimeMillis() - t0} ms")
            } catch (failure: Throwable) {
                // What to do here...
                Timber.tag(loggerTag.value).e("shareUserDevicesKey() : Failed to share <${session.sessionId}>")
            }
        } else {
            Timber.tag(loggerTag.value).i("shareUserDevicesKey() : no need to share key")
        }

        if (noOlmToNotify.isNotEmpty()) {
            // XXX offload?, as they won't read the message anyhow?
            notifyKeyWithHeld(
                    noOlmToNotify,
                    session.sessionId,
                    olmDevice.deviceCurve25519Key,
                    WithHeldCode.NO_OLM
            )
        }
    }

    private suspend fun notifyKeyWithHeld(targets: List<UserDevice>,
                                          sessionId: String,
                                          senderKey: String?,
                                          code: WithHeldCode) {
        Timber.tag(loggerTag.value).d("notifyKeyWithHeld() :sending withheld for session:$sessionId and code $code to" +
                " ${targets.joinToString { "${it.userId}|${it.deviceId}" }}")
        val withHeldContent = RoomKeyWithHeldContent(
                roomId = roomId,
                senderKey = senderKey,
                algorithm = MXCRYPTO_ALGORITHM_MEGOLM,
                sessionId = sessionId,
                codeString = code.value
        )
        val params = SendToDeviceTask.Params(
                EventType.ROOM_KEY_WITHHELD,
                MXUsersDevicesMap<Any>().apply {
                    targets.forEach {
                        setObject(it.userId, it.deviceId, withHeldContent)
                    }
                }
        )
        try {
            withContext(coroutineDispatchers.io) {
                sendToDeviceTask.execute(params)
            }
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value)
                    .e("notifyKeyWithHeld() :$sessionId Failed to send withheld  ${targets.map { "${it.userId}|${it.deviceId}" }}")
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
        map["device_id"] = myDeviceId
        session.useCount++
        return map
    }

    /**
     * Get the list of devices which can encrypt data to.
     * This method must be called in getDecryptingThreadHandler() thread.
     *
     * @param userIds  the user ids whose devices must be checked.
     */
    private suspend fun getDevicesInRoom(userIds: List<String>): DeviceInRoomInfo {
        // We are happy to use a cached version here: we assume that if we already
        // have a list of the user's devices, then we already share an e2e room
        // with them, which means that they will have announced any new devices via
        // an m.new_device.
        val keys = deviceListManager.downloadKeys(userIds, false)
        val encryptToVerifiedDevicesOnly = cryptoStore.getGlobalBlacklistUnverifiedDevices() ||
                cryptoStore.getRoomsListBlacklistUnverifiedDevices().contains(roomId)

        val devicesInRoom = DeviceInRoomInfo()
        val unknownDevices = MXUsersDevicesMap<CryptoDeviceInfo>()

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
                    devicesInRoom.withHeldDevices.setObject(userId, deviceId, WithHeldCode.BLACKLISTED)
                    continue
                }

                if (!deviceInfo.isVerified && encryptToVerifiedDevicesOnly) {
                    devicesInRoom.withHeldDevices.setObject(userId, deviceId, WithHeldCode.UNVERIFIED)
                    continue
                }

                if (deviceInfo.identityKey() == olmDevice.deviceCurve25519Key) {
                    // Don't bother sending to ourself
                    continue
                }
                devicesInRoom.allowedDevices.setObject(userId, deviceId, deviceInfo)
            }
        }
        if (unknownDevices.isEmpty) {
            return devicesInRoom
        } else {
            throw MXCryptoError.UnknownDevice(unknownDevices)
        }
    }

    override suspend fun reshareKey(groupSessionId: String,
                                    userId: String,
                                    deviceId: String,
                                    senderKey: String): Boolean {
        Timber.tag(loggerTag.value).i("process reshareKey for $groupSessionId to $userId:$deviceId")
        val deviceInfo = cryptoStore.getUserDevice(userId, deviceId) ?: return false
                .also { Timber.tag(loggerTag.value).w("reshareKey: Device not found") }

        // Get the chain index of the key we previously sent this device
        val wasSessionSharedWithUser = cryptoStore.getSharedSessionInfo(roomId, groupSessionId, deviceInfo)
        if (!wasSessionSharedWithUser.found) {
            // This session was never shared with this user
            // Send a room key with held
            notifyKeyWithHeld(listOf(UserDevice(userId, deviceId)), groupSessionId, senderKey, WithHeldCode.UNAUTHORISED)
            Timber.tag(loggerTag.value).w("reshareKey: ERROR : Never shared megolm with this device")
            return false
        }
        // if found chain index should not be null
        val chainIndex = wasSessionSharedWithUser.chainIndex ?: return false
                .also {
                    Timber.tag(loggerTag.value).w("reshareKey: Null chain index")
                }

        val devicesByUser = mapOf(userId to listOf(deviceInfo))
        val usersDeviceMap = try {
            ensureOlmSessionsForDevicesAction.handle(devicesByUser)
        } catch (failure: Throwable) {
            null
        }
        val olmSessionResult = usersDeviceMap?.getObject(userId, deviceId)
        if (olmSessionResult?.sessionId == null) {
            Timber.tag(loggerTag.value).w("reshareKey: no session with this device, probably because there were no one-time keys")
            return false
        }
        Timber.tag(loggerTag.value).i(" reshareKey: $groupSessionId:$chainIndex with device $userId:$deviceId using session ${olmSessionResult.sessionId}")

        val sessionHolder = try {
            olmDevice.getInboundGroupSession(groupSessionId, senderKey, roomId)
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value).e(failure, "shareKeysWithDevice: failed to get session $groupSessionId")
            return false
        }

        val export = sessionHolder.mutex.withLock {
            sessionHolder.wrapper.exportKeys()
        } ?: return false.also {
            Timber.tag(loggerTag.value).e("shareKeysWithDevice: failed to export group session $groupSessionId")
        }

        val payloadJson = mapOf(
                "type" to EventType.FORWARDED_ROOM_KEY,
                "content" to export
        )

        val encodedPayload = messageEncrypter.encryptMessage(payloadJson, listOf(deviceInfo))
        val sendToDeviceMap = MXUsersDevicesMap<Any>()
        sendToDeviceMap.setObject(userId, deviceId, encodedPayload)
        Timber.tag(loggerTag.value).i("reshareKey() : sending session $groupSessionId to $userId:$deviceId")
        val sendToDeviceParams = SendToDeviceTask.Params(EventType.ENCRYPTED, sendToDeviceMap)
        return try {
            sendToDeviceTask.execute(sendToDeviceParams)
            Timber.tag(loggerTag.value).i("reshareKey() : successfully send <$groupSessionId> to $userId:$deviceId")
            true
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value).e(failure, "reshareKey() : fail to send <$groupSessionId> to $userId:$deviceId")
            false
        }
    }

    data class DeviceInRoomInfo(
            val allowedDevices: MXUsersDevicesMap<CryptoDeviceInfo> = MXUsersDevicesMap(),
            val withHeldDevices: MXUsersDevicesMap<WithHeldCode> = MXUsersDevicesMap()
    )

    data class UserDevice(
            val userId: String,
            val deviceId: String
    )
}
