package de.spiritcroc.matrixsdk

/**
 * Allows us to set some variables globally where Element hadn't planned
 * to make them available, to save us some work hunting down the
 * many indirections of the Element code, but instead allowing direct access
 * from the Matrix SDK.
 */
 object StaticScSdkHelper {

    var scSdkPreferenceProvider: ScSdkPreferenceProvider? = null

    interface ScSdkPreferenceProvider {
        // RoomSummary preferences
        fun roomUnreadKind(isDirect: Boolean): Int
        fun aggregateUnreadRoomCounts(): Boolean
        fun includeSpaceMembersAsSpaceRooms(): Boolean
    }

}
