/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.model

class MXUsersDevicesMap<E> {

    // A map of maps (userId -> (deviceId -> Object)).
    val map = HashMap<String /* userId */, HashMap<String /* deviceId */, E>>()

    /**
     * @return the user Ids
     */
    val userIds: List<String>
        get() = map.keys.toList()

    val isEmpty: Boolean
        get() = map.isEmpty()

    /**
     * Provides the device ids list for a user id.
     * FIXME Should maybe return emptyList and not null, to avoid many !! in the code
     *
     * @param userId the user id
     * @return the device ids list
     */
    fun getUserDeviceIds(userId: String?): List<String>? {
        return if (!userId.isNullOrBlank() && map.containsKey(userId)) {
            map[userId]!!.keys.toList()
        } else null
    }

    /**
     * Provides the object for a device id and a user Id.
     *
     * @param userId the user id
     * @param deviceId the device id
     * @return the object
     */
    fun getObject(userId: String?, deviceId: String?): E? {
        return if (!userId.isNullOrBlank() && !deviceId.isNullOrBlank()) {
            map[userId]?.get(deviceId)
        } else null
    }

    /**
     * Set an object for a dedicated user Id and device Id.
     *
     * @param userId the user Id
     * @param deviceId the device id
     * @param o the object to set
     */
    fun setObject(userId: String?, deviceId: String?, o: E?) {
        if (null != o && userId?.isNotBlank() == true && deviceId?.isNotBlank() == true) {
            val devices = map.getOrPut(userId) { HashMap() }
            devices[deviceId] = o
        }
    }

    /**
     * Defines the objects map for a user Id.
     *
     * @param userId the user id
     * @param objectsPerDevices the objects maps
     */
    fun setObjects(userId: String?, objectsPerDevices: Map<String, E>?) {
        if (!userId.isNullOrBlank()) {
            if (null == objectsPerDevices) {
                map.remove(userId)
            } else {
                map[userId] = HashMap(objectsPerDevices)
            }
        }
    }

    /**
     * Removes objects for a dedicated user.
     *
     * @param userId the user id.
     */
    fun removeUserObjects(userId: String?) {
        if (!userId.isNullOrBlank()) {
            map.remove(userId)
        }
    }

    /**
     * Clear the internal dictionary.
     */
    fun removeAllObjects() {
        map.clear()
    }

    /**
     * Add entries from another MXUsersDevicesMap.
     *
     * @param other the other one
     */
    fun addEntriesFromMap(other: MXUsersDevicesMap<E>?) {
        if (null != other) {
            map.putAll(other.map)
        }
    }

    override fun toString(): String {
        return "MXUsersDevicesMap $map"
    }
}

inline fun <T> MXUsersDevicesMap<T>.forEach(action: (String, String, T) -> Unit) {
    userIds.forEach { userId ->
        getUserDeviceIds(userId)?.forEach { deviceId ->
            getObject(userId, deviceId)?.let {
                action(userId, deviceId, it)
            }
        }
    }
}
