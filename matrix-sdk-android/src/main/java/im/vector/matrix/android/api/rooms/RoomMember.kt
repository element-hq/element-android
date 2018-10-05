package im.vector.matrix.android.api.rooms

import im.vector.matrix.android.api.events.UnsignedData

data class RoomMember(
        val membership: Membership,
        val displayDame: String? = null,
        val avatarUrl: String? = null,
        val isDirect: Boolean = false,
        val thirdPartyInvite: Invite? = null,
        val unsignedData: UnsignedData? = null
)
