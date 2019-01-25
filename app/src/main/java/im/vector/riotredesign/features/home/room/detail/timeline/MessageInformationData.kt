package im.vector.riotredesign.features.home.room.detail.timeline

data class MessageInformationData(
        val time: CharSequence? = null,
        val avatarUrl: String?,
        val memberName: CharSequence? = null,
        val showInformation: Boolean = true
)