/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.sync.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import okio.buffer
import okio.source
import org.matrix.android.sdk.api.session.events.model.Event
import java.io.ByteArrayInputStream
import java.lang.NullPointerException

internal class RoomSyncEphemeralLazyJsonAdapter(private val moshi: Moshi) : JsonAdapter<RoomSyncEphemeral>() {

    companion object {
        val NAMES = JsonReader.Options.of("events")
    }


    private val lazyEventParser = LazyEventParser(moshi)

    @FromJson
    override fun fromJson(reader: JsonReader): RoomSyncEphemeral? {
         var lazySource: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(RoomSyncTimeLineLazyJsonAdapter.NAMES)) {
                0 -> {
                    lazySource = reader.nextSource().readUtf8()
                    //events = lazyEventParser.parse(JsonReader.of(ByteArrayInputStream(readUtf8.toByteArray()).source().buffer()))
                }
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()

        return object : RoomSyncEphemeral {

            var src = lazySource
            override val events: Sequence<Event>
                get() =  src?.let {
                    lazyEventParser.parse(JsonReader.of(ByteArrayInputStream(it.toByteArray()).source().buffer()))
                } ?: emptySequence()

            override fun release() {
                src  = null
            }
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: RoomSyncEphemeral?) {
        if (value == null) {
            throw NullPointerException("value was null! Wrap in .nullSafe() to write nullable values.")
        }
        val eventAdapter: JsonAdapter<Event> = moshi.adapter(Event::class.java)
        writer.beginObject()
        writer.name("events")
        writer.beginArray()
        value.events.forEach { event ->
            eventAdapter.toJson(writer, event)
        }
        writer.endArray()
        writer.endObject()
    }
}
