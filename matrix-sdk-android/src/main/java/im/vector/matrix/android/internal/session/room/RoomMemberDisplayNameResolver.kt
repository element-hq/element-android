package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.api.session.room.model.RoomMember

class RoomMemberDisplayNameResolver {

    fun resolve(userId: String, members: Map<String, RoomMember>): String? {
        var displayName: String? = null
        val currentMember = members[userId]
        // Get the user display name from the member list of the room
        // Do not consider null display name
        if (currentMember != null && !currentMember.displayName.isNullOrEmpty()) {
            val hasNameCollision = members
                    .filterValues { it != currentMember && it.displayName == currentMember.displayName }
                    .isNotEmpty()
            displayName = if (hasNameCollision) {
                "${currentMember.displayName} ( $userId )"
            } else {
                currentMember.displayName
            }
        }
        // TODO handle invited users
        /*else if (null != member && TextUtils.equals(member!!.membership, RoomMember.MEMBERSHIP_INVITE)) {
            val user = (mDataHandler as MXDataHandler).getUser(userId)
            if (null != user) {
                displayName = user!!.displayname
            }
        }
        */
        if (displayName == null) {
            // By default, use the user ID
            displayName = userId
        }
        return displayName
    }

}