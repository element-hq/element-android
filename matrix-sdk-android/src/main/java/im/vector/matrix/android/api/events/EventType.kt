package im.vector.matrix.android.api.events

import com.squareup.moshi.Json


enum class EventType {

    @Json(name ="m.presence") PRESENCE,
    @Json(name ="m.room.message") MESSAGE,
    @Json(name ="m.sticker") STICKER,
    @Json(name ="m.room.encrypted") ENCRYPTED,
    @Json(name ="m.room.encryption") ENCRYPTION,
    @Json(name ="m.room.message.feedback") FEEDBACK,
    @Json(name ="m.typing") TYPING,
    @Json(name ="m.room.redaction") REDACTION,
    @Json(name ="m.receipt") RECEIPT,
    @Json(name ="m.tag") TAG,
    @Json(name ="m.room_key") ROOM_KEY,
    @Json(name ="m.fully_read") FULLY_READ,
    @Json(name ="m.room.plumbing") PLUMBING,
    @Json(name ="m.room.bot.options") BOT_OPTIONS,
    @Json(name ="m.room_key_request") KEY_REQUEST,
    @Json(name ="m.forwarded_room_key") FORWARDED_ROOM_KEY,
    @Json(name ="org.matrix.room.preview_urls") PREVIEW_URLS,

    // State Events
    @Json(name ="m.room.name") STATE_ROOM_NAME,
    @Json(name ="m.room.topic") STATE_ROOM_TOPIC,
    @Json(name ="m.room.avatar") STATE_ROOM_AVATAR,
    @Json(name ="m.room.member") STATE_ROOM_MEMBER,
    @Json(name ="m.room.third_party_invite") STATE_ROOM_THIRD_PARTY_INVITE,
    @Json(name ="m.room.create") STATE_ROOM_CREATE,
    @Json(name ="m.room.join_rules")  STATE_ROOM_JOIN_RULES,
    @Json(name ="m.room.guest_access") STATE_ROOM_GUEST_ACCESS,
    @Json(name ="m.room.power_levels") STATE_ROOM_POWER_LEVELS,
    @Json(name ="m.room.aliases") STATE_ROOM_ALIASES,
    @Json(name ="m.room.tombstone") STATE_ROOM_TOMBSTONE,
    @Json(name ="m.room.canonical_alias")  STATE_CANONICAL_ALIAS,
    @Json(name ="m.room.history_visibility") STATE_HISTORY_VISIBILITY,
    @Json(name ="m.room.related_groups") STATE_RELATED_GROUPS,
    @Json(name ="m.room.pinned_events") STATE_PINNED_EVENT

}