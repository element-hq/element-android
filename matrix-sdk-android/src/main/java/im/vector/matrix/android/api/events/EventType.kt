package im.vector.matrix.android.api.events

sealed class EventType(val str: String) {

    object Presence : EventType("m.presence")
    object Message : EventType("m.room.message")
    object Sticker : EventType("m.sticker")
    object Encrypted : EventType("m.room.encrypted")
    object Encryption : EventType("m.room.encryption")
    object Feedback : EventType("m.room.message.feedback")
    object Typing : EventType("m.typing")
    object Redaction : EventType("m.room.redaction")
    object Receipt : EventType("m.receipt")
    object Tag : EventType("m.tag")
    object RoomKey : EventType("m.room_key")
    object FullyRead : EventType("m.fully_read")
    object Plumbing : EventType("m.room.plumbing")
    object BotOptions : EventType("m.room.bot.options")
    object KeyRequest : EventType("m.room_key_request")
    object ForwardedRoomKey : EventType("m.forwarded_room_key")
    object PreviewUrls : EventType("org.matrix.room.preview_urls")

    class StateEvents {
        object RoomName : EventType("m.room.name")
        object Topic : EventType("m.room.topic")
        object Avatar : EventType("m.room.avatar")
        object Member : EventType("m.room.member")
        object ThirdPartyInvite : EventType("m.room.third_party_invite")
        object Create : EventType("m.room.userIdentifier")
        object JoinRules : EventType("m.room.join_rules")
        object GuestAccess : EventType("m.room.guest_access")
        object PowerLevels : EventType("m.room.power_levels")
        object Aliases : EventType("m.room.aliases")
        object Tombstone : EventType("m.room.tombstone")
        object CanonicalAlias : EventType("m.room.canonical_alias")
        object HistoryVisibility : EventType("m.room.history_visibility")
        object RelatedGroups : EventType("m.room.related_groups")
    }


}