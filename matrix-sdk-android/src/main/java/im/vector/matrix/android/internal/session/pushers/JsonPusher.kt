/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.internal.session.pushers

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.di.SerializeNulls

/**
 * Example:
 *
 * <code>
 *     {
 *      "pushers": [
 *          {
 *              "pushkey": "Xp/MzCt8/9DcSNE9cuiaoT5Ac55job3TdLSSmtmYl4A=",
 *              "kind": "http",
 *              "app_id": "face.mcapp.appy.prod",
 *              "app_display_name": "Appy McAppface",
 *              "device_display_name": "Alice's Phone",
 *              "profile_tag": "xyz",
 *              "lang": "en-US",
 *              "data": {
 *              "url": "https://example.com/_matrix/push/v1/notify"
 *          }
 *      }]
 *  }
 * </code>
 */

@JsonClass(generateAdapter = true)
internal data class JsonPusher(
        /**
         * Required. This is a unique identifier for this pusher. See /set for more detail. Max length, 512 bytes.
         */
        @Json(name = "pushkey")
        val pushKey: String,

        /**
         * Required. The kind of pusher. "http" is a pusher that sends HTTP pokes.
         */
        @SerializeNulls
        @Json(name = "kind")
        val kind: String?,

        /**
         * Required. This is a reverse-DNS style identifier for the application. Max length, 64 chars.
         */
        @Json(name = "app_id")
        val appId: String,

        /**
         * Required. A string that will allow the user to identify what application owns this pusher.
         */
        @Json(name = "app_display_name")
        val appDisplayName: String? = null,

        /**
         * Required. A string that will allow the user to identify what device owns this pusher.
         */
        @Json(name = "device_display_name")
        val deviceDisplayName: String? = null,

        /**
         * This string determines which set of device specific rules this pusher executes.
         */
        @Json(name = "profile_tag")
        val profileTag: String? = null,

        /**
         * Required. The preferred language for receiving notifications (e.g. 'en' or 'en-US')
         */
        @Json(name = "lang")
        val lang: String? = null,

        /**
         * Required. A dictionary of information for the pusher implementation itself.
         */
        @Json(name = "data")
        val data: JsonPusherData? = null,

        // Only used to update add Pusher (body of api request)
        // Used If true, the homeserver should add another pusher with the given pushkey and App ID in addition
        // to any others with different user IDs.
        // Otherwise, the homeserver must remove any other pushers with the same App ID and pushkey for different users.
        // The default is false.
        @Json(name = "append")
        val append: Boolean? = false
)

