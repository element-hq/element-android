package im.vector.riotredesign.features.home.room.detail.timeline.action

import android.os.Parcelable
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageInformationData
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TimelineEventFragmentArgs(
        val eventId: String,
        val roomId: String,
        val informationData: MessageInformationData
) : Parcelable