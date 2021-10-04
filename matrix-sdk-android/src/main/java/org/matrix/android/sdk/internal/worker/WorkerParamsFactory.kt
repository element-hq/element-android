/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.worker

import androidx.work.Data
import com.squareup.moshi.Moshi
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.network.parsing.CheckNumberType

internal object WorkerParamsFactory {

    private val moshi: Moshi by lazy {
        // We are adding the CheckNumberType as we are serializing/deserializing multiple time in a row
        // and we lost typing information doing so.
        // We don't want this check to be done on all adapters, so we just add it here.
        MoshiProvider.providesMoshi()
                .newBuilder()
                .add(CheckNumberType.JSON_ADAPTER_FACTORY)
                .build()
    }

    private const val KEY = "WORKER_PARAMS_JSON"

    inline fun <reified T> toData(params: T) = toData(T::class.java, params)

    fun <T> toData(clazz: Class<T>, params: T): Data {
        val adapter = moshi.adapter(clazz)
        val json = adapter.toJson(params)
        return Data.Builder().putString(KEY, json).build()
    }

    inline fun <reified T> fromData(data: Data) = fromData(T::class.java, data)

    fun <T> fromData(clazz: Class<T>, data: Data): T? = tryOrNull<T?>("Unable to parse work parameters") {
        val json = data.getString(KEY)
        return if (json == null) {
            null
        } else {
            val adapter = moshi.adapter(clazz)
            adapter.fromJson(json)
        }
    }
}
