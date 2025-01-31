/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync.parsing

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.matrix.android.sdk.api.session.sync.InitialSyncStrategy
import org.matrix.android.sdk.api.session.sync.model.LazyRoomSyncEphemeral
import org.matrix.android.sdk.api.session.sync.model.RoomSyncEphemeral
import org.matrix.android.sdk.internal.session.sync.RoomSyncEphemeralTemporaryStore
import timber.log.Timber

internal class DefaultLazyRoomSyncEphemeralJsonAdapter {

    @FromJson
    fun fromJson(reader: JsonReader, delegate: JsonAdapter<RoomSyncEphemeral>): LazyRoomSyncEphemeral? {
        val roomSyncEphemeral = delegate.fromJson(reader) ?: return null
        return LazyRoomSyncEphemeral.Parsed(roomSyncEphemeral)
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: LazyRoomSyncEphemeral?) {
        // This Adapter is not supposed to serialize object
        Timber.v("To json $value with $writer")
        throw UnsupportedOperationException()
    }
}

internal class SplitLazyRoomSyncEphemeralJsonAdapter(
        private val roomSyncEphemeralTemporaryStore: RoomSyncEphemeralTemporaryStore,
        private val syncStrategy: InitialSyncStrategy.Optimized
) {
    @FromJson
    fun fromJson(reader: JsonReader, delegate: JsonAdapter<RoomSyncEphemeral>): LazyRoomSyncEphemeral? {
        val path = reader.path
        val roomId = path.substringAfter("\$.rooms.join.").substringBeforeLast(".ephemeral")

        val json = reader.nextSource().inputStream().bufferedReader().use {
            it.readText()
        }
        val limit = syncStrategy.minSizeToStoreInFile
        return if (json.length > limit) {
            Timber.d("INIT_SYNC $path content length: ${json.length} copy to a file")
            // Copy the source to a file
            roomSyncEphemeralTemporaryStore.write(roomId, json)
            LazyRoomSyncEphemeral.Stored
        } else {
            Timber.d("INIT_SYNC $path content length: ${json.length} parse it now")
            val roomSync = delegate.fromJson(json) ?: return null
            LazyRoomSyncEphemeral.Parsed(roomSync)
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: LazyRoomSyncEphemeral?) {
        // This Adapter is not supposed to serialize object
        Timber.v("To json $value with $writer")
        throw UnsupportedOperationException()
    }
}
