/*
 * Copyright 2019 New Vector Ltd
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
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.network.parsing.CheckNumberType

internal object WorkerParamsFactory {

    val moshi by lazy {
        // We are adding the CheckNumberType as we are serializing/deserializing multiple time in a row
        // and we lost typing information doing so.
        // We don't want this check to be done on all adapters, so we just add it here.
        MoshiProvider.providesMoshi()
                .newBuilder()
                .add(CheckNumberType.JSON_ADAPTER_FACTORY)
                .build()
    }

    const val KEY = "WORKER_PARAMS_JSON"

    inline fun <reified T> toData(params: T): Data {
        val adapter = moshi.adapter(T::class.java)
        val json = adapter.toJson(params)
        return Data.Builder().putString(KEY, json).build()
    }

    inline fun <reified T> fromData(data: Data): T? {
        val json = data.getString(KEY)
        return if (json == null) {
            null
        } else {
            val adapter = moshi.adapter(T::class.java)
            adapter.fromJson(json)
        }
    }
}
