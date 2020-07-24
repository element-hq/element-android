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

import im.vector.matrix.android.api.crypto.RoomEncryptionTrustLevel
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.VersioningState
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

internal open class RoomSummaryEntity(
        @PrimaryKey var roomId: String = "",
        var displayName: String? = "",
        var avatarUrl: String? = "",
        var name: String? = "",
        var topic: String? = "",
        var latestPreviewableEvent: TimelineEventEntity? = null,
        var heroes: RealmList<String> = RealmList(),
        var joinedMembersCount: Int? = 0,
        var invitedMembersCount: Int? = 0,
        var isDirect: Boolean = false,
        var directUserId: String? = null,
        var otherMemberIds: RealmList<String> = RealmList(),
        var notificationCount: Int = 0,
        var highlightCount: Int = 0,
        var readMarkerId: String? = null,
        var hasUnreadMessages: Boolean = false,
        var tags: RealmList<RoomTagEntity> = RealmList(),
        var userDrafts: UserDraftsEntity? = null,
        var breadcrumbsIndex: Int = RoomSummary.NOT_IN_BREADCRUMBS,
        var canonicalAlias: String? = null,
        var aliases: RealmList<String> = RealmList(),
        // this is required for querying
        var flatAliases: String = "",
        var isEncrypted: Boolean = false,
        var encryptionEventTs: Long? = 0,
        var roomEncryptionTrustLevelStr: String? = null,
        var inviterId: String? = null,
        var hasFailedSending: Boolean = false
) : RealmObject() {

    private var membershipStr: String = Membership.NONE.name
    var membership: Membership
        get() {
            return Membership.valueOf(membershipStr)
        }
        set(value) {
            membershipStr = value.name
        }

    private var versioningStateStr: String = VersioningState.NONE.name
    var versioningState: VersioningState
        get() {
            return VersioningState.valueOf(versioningStateStr)
        }
        set(value) {
            versioningStateStr = value.name
        }

    var roomEncryptionTrustLevel: RoomEncryptionTrustLevel?
        get() {
            return roomEncryptionTrustLevelStr?.let {
                try {
                    RoomEncryptionTrustLevel.valueOf(it)
                } catch (failure: Throwable) {
                    null
                }
            }
        }
        set(value) {
            roomEncryptionTrustLevelStr = value?.name
        }

    companion object
}
