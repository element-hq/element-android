/*
 * Copyright (c) 2021 New Vector Ltd
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

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.matrix.android.sdk.api.util.Extended
import org.matrix.android.sdk.api.util.JSON_DICT_PARAMETERIZED_TYPE
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.JsonKeys
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal class ExtendedAdapterFactory() : JsonAdapter.Factory {

    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) != Extended::class.java) {
            return null
        }
        val wrappedType = (type as ParameterizedType).actualTypeArguments.firstOrNull() ?: return null
        val rawType = Types.getRawType(wrappedType)
        val adapter = moshi.adapter(rawType)
        return ExtendedAdapter(rawType, adapter, moshi.adapter(JSON_DICT_PARAMETERIZED_TYPE))
    }
}

internal class ExtendedAdapter(private val rawType: Class<*>,
                               private val wrappedAdapter: JsonAdapter<*>,
                               private val jsonDictAdapter: JsonAdapter<JsonDict>) : JsonAdapter<Extended<*>>() {

    override fun fromJson(reader: JsonReader): Extended<*>? {
        val wrapped = wrappedAdapter.fromJson(reader.peekJson()) ?: return null
        val allValues = jsonDictAdapter.fromJson(reader) ?: return null
        val definedKeys = JsonKeys.jsonKeysByClasses[rawType].orEmpty()
        val filteredValues = allValues.filterNot {
            definedKeys.contains(it.key)
        }
        return Extended(wrapped, filteredValues)
    }

    override fun toJson(writer: JsonWriter, value: Extended<*>?) {
        // WILL SEE
    }
}
