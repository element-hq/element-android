/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.sync.parsing

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.matrix.android.sdk.internal.session.sync.InitialSyncStrategy
import org.matrix.android.sdk.internal.session.sync.model.LazyRoomSyncEphemeral
import org.matrix.android.sdk.internal.session.sync.model.RoomSyncEphemeral
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

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

internal class SplitLazyRoomSyncJsonAdapter(
        private val workingDirectory: File,
        private val syncStrategy: InitialSyncStrategy.Optimized
) {
    private val atomicInteger = AtomicInteger(0)

    private fun createFile(): File {
        val index = atomicInteger.getAndIncrement()
        return File(workingDirectory, "room_$index.json")
    }

    @FromJson
    fun fromJson(reader: JsonReader, delegate: JsonAdapter<RoomSyncEphemeral>): LazyRoomSyncEphemeral? {
        val path = reader.path
        val json = reader.nextSource().inputStream().bufferedReader().use {
            it.readText()
        }
        val limit = syncStrategy.minSizeToStoreInFile
        return if (json.length > limit) {
            Timber.v("INIT_SYNC $path content length: ${json.length} copy to a file")
            // Copy the source to a file
            val file = createFile()
            file.writeText(json)
            LazyRoomSyncEphemeral.Stored(delegate, file)
        } else {
            Timber.v("INIT_SYNC $path content length: ${json.length} parse it now")
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
