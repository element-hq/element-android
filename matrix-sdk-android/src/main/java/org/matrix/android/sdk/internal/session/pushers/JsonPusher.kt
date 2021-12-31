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
package org.matrix.android.sdk.internal.session.pushers

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.di.SerializeNulls
import java.security.InvalidParameterException

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
         * Required. This is a unique identifier for this pusher. The value you should use for this is the routing or
         * destination address information for the notification, for example, the APNS token for APNS or the
         * Registration ID for GCM. If your notification client has no such concept, use any unique identifier.
         * Max length, 512 bytes.
         *
         * If the kind is "email", this is the email address to send notifications to.
         */
        @Json(name = "pushkey")
        val pushKey: String,

        /**
         * Required. The kind of pusher to configure.
         * "http" makes a pusher that sends HTTP pokes.
         * "email" makes a pusher that emails the user with unread notifications.
         * null deletes the pusher.
         */
        @SerializeNulls
        @Json(name = "kind")
        val kind: String?,

        /**
         * Required. This is a reverse-DNS style identifier for the application. It is recommended that this end
         * with the platform, such that different platform versions get different app identifiers.
         * Max length, 64 chars.
         *
         * If the kind is "email", this is "m.email".
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
         * If kind is http, this should contain url which is the URL to use to send notifications to.
         */
        @Json(name = "data")
        val data: JsonPusherData? = null,

        /**
         * If true, the homeserver should add another pusher with the given pushkey and App ID in addition to any others
         * with different user IDs. Otherwise, the homeserver must remove any other pushers with the same App ID and pushkey
         * for different users.
         * The default is false.
         */
        @Json(name = "append")
        val append: Boolean? = false
) {
    init {
        // Do some parameter checks. It's ok to throw Exception, to inform developer of the problem
        if (pushKey.length > 512) throw InvalidParameterException("pushkey should not exceed 512 chars")
        if (appId.length > 64) throw InvalidParameterException("appId should not exceed 64 chars")
        data?.url?.let { url -> if ("/_matrix/push/v1/notify" !in url) throw InvalidParameterException("url should contain '/_matrix/push/v1/notify'") }
    }
}
