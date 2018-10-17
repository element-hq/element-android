package im.vector.matrix.android.api.session.events.model


object EventType {

    const val PRESENCE = "m.presence"
    const val MESSAGE = "m.room.message"
    const val STICKER = "m.sticker"
    const val ENCRYPTED = "m.room.encrypted"
    const val ENCRYPTION = "m.room.encryption"
    const val FEEDBACK = "m.room.message.feedback"
    const val TYPING = "m.typing"
    const val REDACTION = "m.room.redaction"
    const val RECEIPT = "m.receipt"
    const val TAG = "m.tag"
    const val ROOM_KEY = "m.room_key"
    const val FULLY_READ = "m.fully_read"
    const val PLUMBING = "m.room.plumbing"
    const val BOT_OPTIONS = "m.room.bot.options"
    const val KEY_REQUEST = "m.room_key_request"
    const val FORWARDED_ROOM_KEY = "m.forwarded_room_key"
    const val PREVIEW_URLS = "org.matrix.room.preview_urls"

    // State Events

    const val STATE_ROOM_NAME = "m.room.name"
    const val STATE_ROOM_TOPIC = "m.room.topic"
    const val STATE_ROOM_AVATAR = "m.room.avatar"
    const val STATE_ROOM_MEMBER = "m.room.member"
    const val STATE_ROOM_THIRD_PARTY_INVITE = "m.room.third_party_invite"
    const val STATE_ROOM_CREATE = "m.room.create"
    const val STATE_ROOM_JOIN_RULES = "m.room.join_rules"
    const val STATE_ROOM_GUEST_ACCESS = "m.room.guest_access"
    const val STATE_ROOM_POWER_LEVELS = "m.room.power_levels"
    const val STATE_ROOM_ALIASES = "m.room.aliases"
    const val STATE_ROOM_TOMBSTONE = "m.room.tombstone"
    const val STATE_CANONICAL_ALIAS = "m.room.canonical_alias"
    const val STATE_HISTORY_VISIBILITY = "m.room.history_visibility"
    const val STATE_RELATED_GROUPS = "m.room.related_groups"
    const val STATE_PINNED_EVENT = "m.room.pinned_events"

    // Call Events

    const val CALL_INVITE = "m.call.invite"
    const val CALL_CANDIDATES = "m.call.candidates"
    const val CALL_ANSWER = "m.call.answer"
    const val CALL_HANGUP = "m.call.hangup"

}