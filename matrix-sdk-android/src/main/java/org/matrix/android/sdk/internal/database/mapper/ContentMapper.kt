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

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.util.JSON_DICT_PARAMETERIZED_TYPE
import org.matrix.android.sdk.internal.di.MoshiProvider

internal object ContentMapper {

    private val moshi = MoshiProvider.providesMoshi()
    private val adapter = moshi.adapter<Content>(JSON_DICT_PARAMETERIZED_TYPE)

    fun map(content: String?): Content? {
        return content?.let {
            adapter.fromJson(it)
        }
    }

    fun map(content: Content?): String? {
        return content?.let {
            adapter.toJson(it)
        }
    }
}
