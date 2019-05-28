package im.vector.matrix.android.api.session.room.model.relation

interface RelationContent {
    val type: String?
    val eventId: String?
    val inReplyTo: ReplyToContent?
}