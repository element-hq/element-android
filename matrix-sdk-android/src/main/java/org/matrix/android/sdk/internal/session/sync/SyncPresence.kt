/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
    Busy("busy"),
    Unavailable("unavailable");

    companion object {
        fun from(presenceEnum: PresenceEnum): SyncPresence {
            return when (presenceEnum) {
                PresenceEnum.ONLINE -> Online
                PresenceEnum.OFFLINE -> Offline
                PresenceEnum.BUSY -> Busy
                PresenceEnum.UNAVAILABLE -> Unavailable
            }
        }

        fun from(s: String?): SyncPresence? = values().find { it.value == s }
    }
}
