package im.vector.matrix.android.api.session.room.model.message

object MessageType {

    val MSGTYPE_TEXT = "m.text"
    val MSGTYPE_EMOTE = "m.emote"
    val MSGTYPE_NOTICE = "m.notice"
    val MSGTYPE_IMAGE = "m.image"
    val MSGTYPE_AUDIO = "m.audio"
    val MSGTYPE_VIDEO = "m.video"
    val MSGTYPE_LOCATION = "m.location"
    val MSGTYPE_FILE = "m.file"
    val FORMAT_MATRIX_HTML = "org.matrix.custom.html"
    // Add, in local, a fake message type in order to StickerMessage can inherit Message class
    // Because sticker isn't a message type but a event type without msgtype field
    val MSGTYPE_STICKER_LOCAL = "org.matrix.android.sdk.sticker"
}