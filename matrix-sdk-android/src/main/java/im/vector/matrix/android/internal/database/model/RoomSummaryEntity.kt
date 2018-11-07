package im.vector.matrix.android.internal.database.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class RoomSummaryEntity(@PrimaryKey var roomId: String = "",
                             var displayName: String? = "",
                             var avatarUrl: String? = "",
                             var topic: String? = "",
                             var lastMessage: EventEntity? = null,
                             var heroes: RealmList<String> = RealmList(),
                             var joinedMembersCount: Int? = 0,
                             var invitedMembersCount: Int? = 0,
                             var isDirect: Boolean = false,
                             var isLatestSelected: Boolean = false,
                             var otherMemberIds: RealmList<String> = RealmList()
) : RealmObject() {

    companion object

}