/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.filter

internal object FilterUtil {

    /**
     * Patch the filterBody to enable or disable the data save mode
     *
     * If data save mode is on, FilterBody will contains
     * FIXME New expected filter:
     * "{\"room\": {\"ephemeral\": {\"notTypes\": [\"m.typing\"]}}, \"presence\":{\"notTypes\": [\"*\"]}}"
     *
     * @param filterBody filterBody to patch
     * @param useDataSaveMode true to enable data save mode
     */
    /*
    fun enableDataSaveMode(filterBody: FilterBody, useDataSaveMode: Boolean) {
        if (useDataSaveMode) {
            // Enable data save mode
            if (filterBody.room == null) {
                filterBody.room = RoomFilter()
            }
            filterBody.room?.let { room ->
                if (room.ephemeral == null) {
                    room.ephemeral = RoomEventFilter()
                }
                room.ephemeral?.types?.let { types ->
                    if (!types.contains("m.receipt")) {
                        types.add("m.receipt")
                    }
                }
            }

            if (filterBody.presence == null) {
                filterBody.presence = Filter()
            }
            filterBody.presence?.notTypes?.let { notTypes ->
                if (!notTypes.contains("*")) {
                    notTypes.add("*")
                }
            }
        } else {
            filterBody.room?.let { room ->
                room.ephemeral?.types?.remove("m.receipt")
                if (room.ephemeral?.types?.isEmpty() == true) {
                    room.ephemeral?.types = null
                }
                if (room.ephemeral?.hasData() == false) {
                    room.ephemeral = null
                }
            }
            if (filterBody.room?.hasData() == false) {
                filterBody.room = null
            }

            filterBody.presence?.let { presence ->
                presence.notTypes?.remove("*")
                if (presence.notTypes?.isEmpty() == true) {
                    presence.notTypes = null
                }
            }
            if (filterBody.presence?.hasData() == false) {
                filterBody.presence = null
            }
        }
    } */

    /**
     * Compute a new filter to enable or disable the lazy loading.
     *
     * If lazy loading is on, the filter will looks like
     * {"room":{"state":{"lazy_load_members":true})}
     *
     * @param filter filter to patch
     * @param useLazyLoading true to enable lazy loading
     */
    fun enableLazyLoading(filter: Filter, useLazyLoading: Boolean): Filter {
        if (useLazyLoading) {
            // Enable lazy loading
            return filter.copy(
                    room = filter.room?.copy(
                            state = filter.room.state?.copy(lazyLoadMembers = true)
                                    ?: RoomEventFilter(lazyLoadMembers = true)
                    )
                            ?: RoomFilter(state = RoomEventFilter(lazyLoadMembers = true))
            )
        } else {
            val newRoomEventFilter = filter.room?.state?.copy(lazyLoadMembers = null)?.takeIf { it.hasData() }
            val newRoomFilter = filter.room?.copy(state = newRoomEventFilter)?.takeIf { it.hasData() }

            return filter.copy(
                    room = newRoomFilter
            )
        }
    }
}
