/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.pushers

data class HttpPusher(
        /**
         * This is a unique identifier for this pusher. The value you should use for
         * this is the routing or destination address information for the notification,
         * for example, the APNS token for APNS or the Registration ID for GCM. If your
         * notification client has no such concept, use any unique identifier. Max length, 512 chars.
         */
        val pushkey: String,

        /**
         * The application id
         * This is a reverse-DNS style identifier for the application. It is recommended
         * that this end with the platform, such that different platform versions get
         * different app identifiers. Max length, 64 chars.
         */
        val appId: String,

        /**
         * This string determines which set of device specific rules this pusher executes.
         */
        val profileTag: String,

        /**
         * The preferred language for receiving notifications (e.g. "en" or "en-US").
         */
        val lang: String,

        /**
         * A human readable string that will allow the user to identify what application owns this pusher.
         */
        val appDisplayName: String,

        /**
         * A human readable string that will allow the user to identify what device owns this pusher.
         */
        val deviceDisplayName: String,

        /**
         * The URL to use to send notifications to. MUST be an HTTPS URL with a path of /_matrix/push/v1/notify.
         */
        val url: String,

        /**
         * If true, the homeserver should add another pusher with the given pushkey and App ID in addition
         * to any others with different user IDs. Otherwise, the homeserver must remove any other pushers
         * with the same App ID and pushkey for different users.
         */
        val append: Boolean,

        /**
         * true to limit the push content to only id and not message content
         * Ref: https://matrix.org/docs/spec/push_gateway/r0.1.1#homeserver-behaviour
         */
        val withEventIdOnly: Boolean
)
