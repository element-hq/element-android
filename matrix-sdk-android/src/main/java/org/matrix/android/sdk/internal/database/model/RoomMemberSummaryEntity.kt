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

package org.matrix.android.sdk.internal.database.model

import org.matrix.android.sdk.api.session.room.model.Membership
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

internal open class RoomMemberSummaryEntity(@PrimaryKey var primaryKey: String = "",
                                            @Index var userId: String = "",
                                            @Index var roomId: String = "",
                                            @Index var displayName: String? = null,
                                            var avatarUrl: String? = null,
                                            var reason: String? = null,
                                            var isDirect: Boolean = false
) : RealmObject() {

    private var membershipStr: String = Membership.NONE.name
    var membership: Membership
        get() {
            return Membership.valueOf(membershipStr)
        }
        set(value) {
            membershipStr = value.name
        }

    fun getBestName() = displayName?.takeIf { it.isNotBlank() } ?: userId

    companion object
}
