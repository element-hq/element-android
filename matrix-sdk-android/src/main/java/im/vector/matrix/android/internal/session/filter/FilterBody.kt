/*
 * Copyright 2018 Matthias Kesler
 * Copyright 2018 New Vector Ltd
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
package im.vector.matrix.android.internal.session.filter

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.di.MoshiProvider


/**
 * Class which can be parsed to a filter json string. Used for POST and GET
 * Have a look here for further information:
 * https://matrix.org/docs/spec/client_server/r0.3.0.html#post-matrix-client-r0-user-userid-filter
 */
@JsonClass(generateAdapter = true)
internal data class FilterBody(
        @Json(name = "event_fields") var eventFields: List<String>? = null,
        @Json(name = "event_format") var eventFormat: String? = null,
        @Json(name = "presence") var presence: Filter? = null,
        @Json(name = "account_data") var accountData: Filter? = null,
        @Json(name = "room") var room: RoomFilter? = null
) {

    fun toJSONString(): String {
        return MoshiProvider.providesMoshi().adapter(FilterBody::class.java).toJson(this)
    }
}
