package im.vector.app.features.roomprofile.members

import javax.inject.Inject

class RoomMemberSummaryWithPowerComparator @Inject constructor(private val roomMemberSummaryComparator: RoomMemberSummaryComparator) : Comparator<RoomMemberListViewModel.RoomMemberSummaryWithPower> {

    override fun compare(leftRoomMemberSummary: RoomMemberListViewModel.RoomMemberSummaryWithPower?, rightRoomMemberSummary: RoomMemberListViewModel.RoomMemberSummaryWithPower?): Int {
        return if (leftRoomMemberSummary == null || rightRoomMemberSummary == null || leftRoomMemberSummary.powerLevel == rightRoomMemberSummary.powerLevel) {
            roomMemberSummaryComparator.compare(leftRoomMemberSummary?.roomMemberSummary, rightRoomMemberSummary?.roomMemberSummary)
        } else {
            rightRoomMemberSummary.powerLevel - leftRoomMemberSummary.powerLevel
        }
    }
}
