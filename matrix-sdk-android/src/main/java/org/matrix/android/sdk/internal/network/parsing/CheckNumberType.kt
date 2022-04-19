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

package org.matrix.android.sdk.internal.network.parsing

import androidx.annotation.Nullable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.io.IOException
import java.lang.reflect.Type
import java.math.BigDecimal

/**
 * This is used to check if NUMBER in json is integer or double, so we can preserve typing when serializing/deserializing in a row.
 */
internal interface CheckNumberType {

    companion object {
        val JSON_ADAPTER_FACTORY = object : JsonAdapter.Factory {
            @Nullable
            override fun create(type: Type, annotations: Set<Annotation?>?, moshi: Moshi): JsonAdapter<*>? {
                if (type !== Any::class.java) {
                    return null
                }
                val delegate: JsonAdapter<Any> = moshi.nextAdapter(this, Any::class.java, emptySet())
                return object : JsonAdapter<Any?>() {
                    @Nullable
                    @Throws(IOException::class)
                    override fun fromJson(reader: JsonReader): Any? {
                        return if (reader.peek() !== JsonReader.Token.NUMBER) {
                            delegate.fromJson(reader)
                        } else {
                            val numberAsString = reader.nextString()
                            val decimal = BigDecimal(numberAsString)
                            if (decimal.scale() <= 0) {
                                decimal.longValueExact()
                            } else {
                                decimal.toDouble()
                            }
                        }
                    }

                    override fun toJson(writer: JsonWriter, value: Any?) {
                        delegate.toJson(writer, value)
                    }
                }
            }
        }
    }
}
