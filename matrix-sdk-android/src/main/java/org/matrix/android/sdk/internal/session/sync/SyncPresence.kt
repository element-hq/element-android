/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.sync

import org.matrix.android.sdk.api.session.presence.model.PresenceEnum

/**
 * For `set_presence` parameter in the /sync request
 *
 * Controls whether the client is automatically marked as online by polling this API. If this parameter
 * is omitted then the client is automatically marked as online when it uses this API. Otherwise if the
 * parameter is set to "offline" then the client is not marked as being online when it uses this API.
 * When set to "unavailable", the client is marked as being idle. One of: ["offline", "online", "unavailable"]
 */
internal enum class SyncPresence(val value: String) {
    Offline("offline"),
    Online("online"),
    Unavailable("unavailable");

    companion object {
        fun from(presenceEnum: PresenceEnum): SyncPresence {
            return when (presenceEnum) {
                PresenceEnum.ONLINE -> Online
                PresenceEnum.OFFLINE -> Offline
                PresenceEnum.UNAVAILABLE -> Unavailable
            }
        }
        fun from(s: String?): SyncPresence? = values().find { it.value == s }
    }
}
