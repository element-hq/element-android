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

package im.vector.matrix.android.internal.database.model

import im.vector.matrix.android.api.session.room.model.Membership
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import kotlin.properties.Delegates

internal open class RoomSummaryEntity(@PrimaryKey var roomId: String = "",
                                      var displayName: String? = "",
                                      var avatarUrl: String? = "",
                                      var topic: String? = "",
                                      var latestEvent: EventEntity? = null,
                                      var heroes: RealmList<String> = RealmList(),
                                      var joinedMembersCount: Int? = 0,
                                      var invitedMembersCount: Int? = 0,
                                      var isDirect: Boolean = false,
                                      var otherMemberIds: RealmList<String> = RealmList(),
                                      var notificationCount: Int = 0,
                                      var highlightCount: Int = 0,
                                      var tags: RealmList<RoomTagEntity> = RealmList()
) : RealmObject() {

    private var membershipStr: String = Membership.NONE.name

    @delegate:Ignore
    var membership: Membership by Delegates.observable(Membership.valueOf(membershipStr)) { _, _, newValue ->
        membershipStr = newValue.name
    }

    companion object

}