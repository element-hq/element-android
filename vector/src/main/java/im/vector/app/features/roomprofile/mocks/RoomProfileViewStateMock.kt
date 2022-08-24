package im.vector.app.features.roomprofile.mocks

import com.airbnb.mvrx.Success
import im.vector.app.features.roomprofile.RoomProfileViewState
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.UnsignedData
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomEncryptionAlgorithm
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.VersioningState
import org.matrix.android.sdk.api.session.room.model.create.Predecessor
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

val mockRoomProfileViewState by lazy { RoomProfileViewState(
roomId = "!CWLUCoEWXSFyTCOtfL:matrix.org",
roomSummary = Success(
value = RoomSummary(
roomId = "!CWLUCoEWXSFyTCOtfL:matrix.org",
displayName = "Megolm test (E2E Encryption Testfest)",
name = "Megolm test (E2E Encryption Testfest)",
topic = "Test room! Please discuss issues at #e2e:matrix.org. Requires synapse 0.18.0 or later. (0.18.4 for encrypted images); Latest version is recommended.",
avatarUrl = "",
canonicalAlias = "#megolm:matrix.org",
joinRules = RoomJoinRules.PUBLIC,
joinedMembersCount = 199,
latestPreviewableEvent = TimelineEvent(
root = Event(
type = "m.room.encrypted",
eventId = "\$zo2MIeXtdl9dWNhz1cbY3YT55rlpQLG8XpYBTFN-JXI",
content = mutableMapOf("algorithm" to "m.megolm.v1.aes-sha2","ciphertext" to "AwgAEoADprWrq3ZJ4BLD/w0+Lx9H/3mXZpnEQdgbF2FoWOPB1fuki6/1w7vikL1Sb6rP5AwMuA4TPJQ14Pl++/TCGCUzRoZE9m8TsYe0nQXKb2VWRDB6u7i1ib27l3epbtQAIOYfKuCg6MnbMmadef9fEA1K4lHl3xxf0SdWF9PQKGq3nf9rhcZwa3AZW+FDepI094NO98bTHmssKJ5YTp1ChStjALy4FwdcfanlD5WWPHbKqij/RIC63FNAspIOUeb3AkBmcAdoUctBrxXml+NpJKKCGKxXvFudeSkiqHMG","device_id" to "QBOXOIATIX","sender_key" to "dGWOgnrcFYOdsYAfwvKJQcDr4dwuZ0R743B3ZlGSMWU","session_id" to "Eg1c1yyhczbhGt2baH4m6qI4TpJxN0Kb59TxZ3JY5lE"),
originServerTs = 1661232614808,
senderId = "@DatseMultimedia:matrix.org",
roomId = "!CWLUCoEWXSFyTCOtfL:matrix.org",
unsignedData = UnsignedData(age = 7112957)
),
localId = 133,
eventId = "\$zo2MIeXtdl9dWNhz1cbY3YT55rlpQLG8XpYBTFN-JXI",
displayIndex = 11,
senderInfo = SenderInfo(
userId = "@DatseMultimedia:matrix.org",
displayName = "Jigme Datse (they/them)",
isUniqueDisplayName = true,
avatarUrl = "mxc://matrix.org/aIPbYElyzjKrOWPaWkxbWEqy"
)
),
otherMemberIds = listOf(
"@zocker1999:matrix.org",
"@denisea:element.io",
"@+:jae.fi"
),
membership = Membership.JOIN,
versioningState = VersioningState.NONE,
readMarkerId = "\$-wLehwJjk4gGG8igOfp-giNzGQruvlZMI-Vti1YYxHA",
isEncrypted = true,
encryptionEventTs = 1574382534883,
typingUsers = listOf(

),
roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Default,
roomEncryptionAlgorithm = RoomEncryptionAlgorithm.Megolm
)
),
roomCreateContent = Success(
value = RoomCreateContent(
creator = "@abuse:matrix.org",
roomVersion = "5",
predecessor = Predecessor(roomId = "!UCnwUWwIKhcpaPTHtR:sw1v.org",eventId = "\$1574382534206ivbfT:matrix.org")
)
),
bannedMembership = Success(value = listOf(

)),
actionPermissions = RoomProfileViewState.ActionPermissions(),
recommendedRoomVersion = "9"
) }
