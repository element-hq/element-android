package im.vector.matrix.android.api.session.room.model

data class ReadReceipt(
        val userId: String,
        val eventId: String,
        val originServerTs: Long
)