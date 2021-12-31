/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database.model.presence

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.matrix.android.sdk.api.session.presence.model.PresenceEnum
import org.matrix.android.sdk.api.session.presence.model.UserPresence

internal open class UserPresenceEntity(@PrimaryKey var userId: String = "",
                                       var lastActiveAgo: Long? = null,
                                       var statusMessage: String? = null,
                                       var isCurrentlyActive: Boolean? = null,
                                       var avatarUrl: String? = null,
                                       var displayName: String? = null
) : RealmObject() {

    var presence: PresenceEnum
        get() {
            return PresenceEnum.valueOf(presenceStr)
        }
        set(value) {
            presenceStr = value.name
        }

    private var presenceStr: String = PresenceEnum.UNAVAILABLE.name

    companion object
}

internal fun UserPresenceEntity.toUserPresence() =
        UserPresence(
                lastActiveAgo,
                statusMessage,
                isCurrentlyActive,
                presence
        )
