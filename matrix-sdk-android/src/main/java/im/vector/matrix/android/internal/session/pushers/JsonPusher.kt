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
 * pushkey 	string 	Required. This is a unique identifier for this pusher. See /set for more detail. Max length, 512 bytes.
 * kind 	string 	Required. The kind of pusher. "http" is a pusher that sends HTTP pokes.
 * app_id 	string 	Required. This is a reverse-DNS style identifier for the application. Max length, 64 chars.
 * app_display_name 	string 	Required. A string that will allow the user to identify what application owns this pusher.
 * device_display_name 	string 	Required. A string that will allow the user to identify what device owns this pusher.
 * profile_tag 	string 	This string determines which set of device specific rules this pusher executes.
 * lang 	string 	Required. The preferred language for receiving notifications (e.g. 'en' or 'en-US')
 * data 	PusherData 	Required. A dictionary of information for the pusher implementation itself.
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
        @Json(name = "pushkey") val pushKey: String,
        @Json(name = "kind") @SerializeNulls val kind: String?,
        @Json(name = "app_id") val appId: String,
        @Json(name = "app_display_name") val appDisplayName: String? = null,
        @Json(name = "device_display_name") val deviceDisplayName: String? = null,
        @Json(name = "profile_tag") val profileTag: String? = null,
        @Json(name = "lang") val lang: String? = null,
        @Json(name = "data") val data: JsonPusherData? = null,

        // Only used to update add Pusher (body of api request)
        // Used If true, the homeserver should add another pusher with the given pushkey and App ID in addition
        // to any others with different user IDs.
        // Otherwise, the homeserver must remove any other pushers with the same App ID and pushkey for different users.
        // The default is false.
        @Json(name = "append") val append: Boolean? = false
)

