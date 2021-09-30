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
package org.matrix.android.sdk.api.session.pushers

data class Pusher(
        val pushKey: String,
        val kind: String,
        val appId: String,
        val appDisplayName: String?,
        val deviceDisplayName: String?,
        val profileTag: String? = null,
        val lang: String?,
        val data: PusherData,

        val state: PusherState
) {
    companion object {

        const val KIND_EMAIL = "email"
        const val KIND_HTTP = "http"
        const val APP_ID_EMAIL = "m.email"
    }
}

enum class PusherState {
    UNREGISTERED,
    REGISTERING,
    UNREGISTERING,
    REGISTERED,
    FAILED_TO_REGISTER
}

data class PusherData(
        val url: String? = null,
        val format: String? = null
)
