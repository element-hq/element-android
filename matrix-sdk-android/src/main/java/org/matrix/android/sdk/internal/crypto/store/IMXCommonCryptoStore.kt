/*
 * Copyright 2023 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.store

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.crypto.GlobalCryptoConfig
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.CryptoRoomInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.events.model.content.EncryptionEventContent
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.crypto.store.db.CryptoStoreAggregator

/**
 * As a temporary measure rust and kotlin flavor are still using realm to store some crypto
 * related information. In the near future rust flavor will complitly stop using realm, as soon
 * as the missing bits are store in rust side (like room encryption settings, ..)
 * This interface defines what's now used by both flavors.
 * The actual implementation are moved in each flavors
 */
interface IMXCommonCryptoStore {

    /**
     * Provides the algorithm used in a dedicated room.
     *
     * @param roomId the room id
     * @return the algorithm, null is the room is not encrypted
     */
    fun getRoomAlgorithm(roomId: String): String?

    fun getRoomCryptoInfo(roomId: String): CryptoRoomInfo?

    fun setAlgorithmInfo(roomId: String, encryption: EncryptionEventContent?)

    fun roomWasOnceEncrypted(roomId: String): Boolean

    fun saveMyDevicesInfo(info: List<DeviceInfo>)

    // questionable that it's stored in crypto store
    fun getMyDevicesInfo(): List<DeviceInfo>

    // questionable that it's stored in crypto store
    fun getLiveMyDevicesInfo(): LiveData<List<DeviceInfo>>

    // questionable that it's stored in crypto store
    fun getLiveMyDevicesInfo(deviceId: String): LiveData<Optional<DeviceInfo>>

    /**
     * open any existing crypto store.
     */
    fun open()
    fun tidyUpDataBase()

    /**
     * Close the store.
     */
    fun close()

    /*
     * Store a bunch of data collected during a sync response treatment. @See [CryptoStoreAggregator].
     */
    fun storeData(cryptoStoreAggregator: CryptoStoreAggregator)

    fun shouldEncryptForInvitedMembers(roomId: String): Boolean

    /**
     * Sets a boolean flag that will determine whether or not room history (existing inbound sessions)
     * will be shared to new user invites.
     *
     * @param roomId the room id
     * @param shouldShareHistory The boolean flag
     */
    fun setShouldShareHistory(roomId: String, shouldShareHistory: Boolean)

    /**
     * Sets a boolean flag that will determine whether or not this device should encrypt Events for
     * invited members.
     *
     * @param roomId the room id
     * @param shouldEncryptForInvitedMembers The boolean flag
     */
    fun setShouldEncryptForInvitedMembers(roomId: String, shouldEncryptForInvitedMembers: Boolean)

    /**
     * Define if encryption keys should be sent to unverified devices in this room.
     *
     * @param roomId the roomId
     * @param block if true will not send keys to unverified devices
     */
    fun blockUnverifiedDevicesInRoom(roomId: String, block: Boolean)

    /**
     * Set the global override for whether the client should ever send encrypted
     * messages to unverified devices.
     * If false, it can still be overridden per-room.
     * If true, it overrides the per-room settings.
     *
     * @param block true to unilaterally blacklist all
     */
    fun setGlobalBlacklistUnverifiedDevices(block: Boolean)

    fun getLiveGlobalCryptoConfig(): LiveData<GlobalCryptoConfig>

    /**
     * @return true to unilaterally blacklist all unverified devices.
     */
    fun getGlobalBlacklistUnverifiedDevices(): Boolean

    /**
     * A live status regarding sharing keys for unverified devices in this room.
     *
     * @return Live status
     */
    fun getLiveBlockUnverifiedDevices(roomId: String): LiveData<Boolean>

    /**
     * Tell if unverified devices should be blacklisted when sending keys.
     *
     * @return true if should not send keys to unverified devices
     */
    fun getBlockUnverifiedDevices(roomId: String): Boolean

    /**
     * Retrieve a device by its identity key.
     *
     * @param userId the device owner
     * @param identityKey the device identity key (`MXDeviceInfo.identityKey`)
     * @return the device or null if not found
     */
    fun deviceWithIdentityKey(userId: String, identityKey: String): CryptoDeviceInfo?
}
