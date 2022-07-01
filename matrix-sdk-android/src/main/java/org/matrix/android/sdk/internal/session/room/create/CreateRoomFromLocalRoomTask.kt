/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.create

import android.util.Patterns
import androidx.core.net.toUri
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import kotlinx.coroutines.TimeoutCancellationException
import org.matrix.android.sdk.api.extensions.ensurePrefix
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomAliasesContent
import org.matrix.android.sdk.api.session.room.model.RoomAvatarContent
import org.matrix.android.sdk.api.session.room.model.RoomCanonicalAliasContent
import org.matrix.android.sdk.api.session.room.model.RoomGuestAccessContent
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibilityContent
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesContent
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.RoomNameContent
import org.matrix.android.sdk.api.session.room.model.RoomTopicContent
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import org.matrix.android.sdk.api.session.room.model.localecho.LocalRoomThirdPartyInviteContent
import org.matrix.android.sdk.api.session.room.model.tombstone.RoomTombstoneContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.database.awaitNotEmptyResult
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.database.query.whereRoomId
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.room.state.StateEventDataSource
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import org.matrix.android.sdk.internal.util.time.Clock
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Create a Room from a "fake" local room.
 * The configuration of the local room will be use to configure the new room.
 * The potential local room members will also be invited to this new room.
 *
 * A "fake" local tombstone event will be created to indicate that the local room has been replacing by the new one.
 */
internal interface CreateRoomFromLocalRoomTask : Task<CreateRoomFromLocalRoomTask.Params, String> {
    data class Params(val localRoomId: String)
}

internal class DefaultCreateRoomFromLocalRoomTask @Inject constructor(
        @UserId private val userId: String,
        @SessionDatabase private val monarchy: Monarchy,
        private val createRoomTask: CreateRoomTask,
        private val stateEventDataSource: StateEventDataSource,
        private val clock: Clock,
) : CreateRoomFromLocalRoomTask {

    private val realmConfiguration
        get() = monarchy.realmConfiguration

    override suspend fun execute(params: CreateRoomFromLocalRoomTask.Params): String {
        val replacementRoomId = stateEventDataSource.getStateEvent(params.localRoomId, EventType.STATE_ROOM_TOMBSTONE, QueryStringValue.IsEmpty)
                ?.content?.toModel<RoomTombstoneContent>()
                ?.replacementRoomId

        if (replacementRoomId != null) {
            return replacementRoomId
        }

        val createRoomParams = getCreateRoomParams(params)
        val roomId = createRoomTask.execute(createRoomParams)

        try {
            // Wait for all the room events before triggering the replacement room
            awaitNotEmptyResult(realmConfiguration, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                realm.where(RoomSummaryEntity::class.java)
                        .equalTo(RoomSummaryEntityFields.ROOM_ID, roomId)
                        .equalTo(RoomSummaryEntityFields.INVITED_MEMBERS_COUNT, createRoomParams.invitedUserIds.size.minus(1))
            }
            awaitNotEmptyResult(realmConfiguration, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                EventEntity.whereRoomId(realm, roomId)
                        .equalTo(EventEntityFields.TYPE, EventType.STATE_ROOM_HISTORY_VISIBILITY)
            }
            if (createRoomParams.algorithm != null) {
                awaitNotEmptyResult(realmConfiguration, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                    EventEntity.whereRoomId(realm, roomId)
                            .equalTo(EventEntityFields.TYPE, EventType.STATE_ROOM_ENCRYPTION)
                }
            }
        } catch (exception: TimeoutCancellationException) {
            throw CreateRoomFailure.CreatedWithTimeout(roomId)
        }

        createTombstoneEvent(params, roomId)
        return roomId
    }

    /**
     * Retrieve the room configuration by parsing the state events related to the local room.
     */
    private suspend fun getCreateRoomParams(params: CreateRoomFromLocalRoomTask.Params): CreateRoomParams {
        var createRoomParams = CreateRoomParams()
        monarchy.awaitTransaction { realm ->
            val stateEvents = CurrentStateEventEntity.whereRoomId(realm, params.localRoomId).findAll()
            stateEvents.forEach { event ->
                createRoomParams = when (event.type) {
                    EventType.STATE_ROOM_MEMBER -> handleRoomMemberEvent(realm, event, createRoomParams)
                    EventType.LOCAL_STATE_ROOM_THIRD_PARTY_INVITE -> handleLocalRoomThirdPartyInviteEvent(realm, event, createRoomParams)
                    EventType.STATE_ROOM_HISTORY_VISIBILITY -> handleRoomHistoryVisibilityEvent(realm, event, createRoomParams)
                    EventType.STATE_ROOM_ALIASES -> handleRoomAliasesEvent(realm, event, createRoomParams)
                    EventType.STATE_ROOM_AVATAR -> handleRoomAvatarEvent(realm, event, createRoomParams)
                    EventType.STATE_ROOM_CANONICAL_ALIAS -> handleRoomCanonicalAliasEvent(realm, event, createRoomParams)
                    EventType.STATE_ROOM_GUEST_ACCESS -> handleRoomGuestAccessEvent(realm, event, createRoomParams)
                    EventType.STATE_ROOM_ENCRYPTION -> handleRoomEncryptionEvent(createRoomParams)
                    EventType.STATE_ROOM_POWER_LEVELS -> handleRoomPowerRoomLevelsEvent(realm, event, createRoomParams)
                    EventType.STATE_ROOM_NAME -> handleRoomNameEvent(realm, event, createRoomParams)
                    EventType.STATE_ROOM_TOPIC -> handleRoomTopicEvent(realm, event, createRoomParams)
                    EventType.STATE_ROOM_THIRD_PARTY_INVITE -> handleRoomThirdPartyInviteEvent(event, createRoomParams)
                    EventType.STATE_ROOM_JOIN_RULES -> handleRoomJoinRulesEvent(realm, event, createRoomParams)
                    else -> createRoomParams
                }
            }
        }
        return createRoomParams
    }

    /**
     * Create a Tombstone event to indicate that the local room has been replaced by a new one.
     */
    private suspend fun createTombstoneEvent(params: CreateRoomFromLocalRoomTask.Params, roomId: String) {
        val now = clock.epochMillis()
        val event = Event(
                type = EventType.STATE_ROOM_TOMBSTONE,
                senderId = userId,
                originServerTs = now,
                stateKey = "",
                eventId = UUID.randomUUID().toString(),
                content = RoomTombstoneContent(
                        replacementRoomId = roomId
                ).toContent()
        )
        monarchy.awaitTransaction { realm ->
            val eventEntity = event.toEntity(params.localRoomId, SendState.SYNCED, now).copyToRealmOrIgnore(realm, EventInsertType.INCREMENTAL_SYNC)
            if (event.stateKey != null && event.type != null && event.eventId != null) {
                CurrentStateEventEntity.getOrCreate(realm, params.localRoomId, event.stateKey, event.type).apply {
                    eventId = event.eventId
                    root = eventEntity
                }
            }
        }
    }

    /* ==========================================================================================
     * Local events handling
     * ========================================================================================== */

    private fun handleRoomMemberEvent(realm: Realm, event: CurrentStateEventEntity, params: CreateRoomParams): CreateRoomParams = params.apply {
        val content = getEventContent<RoomMemberContent>(realm, event.eventId) ?: return@apply
        invitedUserIds.add(event.stateKey)
        if (content.isDirect) {
            setDirectMessage()
        }
    }

    private fun handleLocalRoomThirdPartyInviteEvent(realm: Realm, event: CurrentStateEventEntity, params: CreateRoomParams): CreateRoomParams = params.apply {
        val content = getEventContent<LocalRoomThirdPartyInviteContent>(realm, event.eventId) ?: return@apply
        val threePid = when {
            content.thirdPartyInvite?.email != null -> ThreePid.Email(content.thirdPartyInvite.email)
            content.thirdPartyInvite?.msisdn != null -> ThreePid.Msisdn(content.thirdPartyInvite.msisdn)
            else -> return@apply
        }
        invite3pids.add(threePid)
        if (content.isDirect) {
            setDirectMessage()
        }
    }

    private fun handleRoomHistoryVisibilityEvent(realm: Realm, event: CurrentStateEventEntity, params: CreateRoomParams): CreateRoomParams = params.apply {
        val content = getEventContent<RoomHistoryVisibilityContent>(realm, event.eventId) ?: return@apply
        historyVisibility = content.historyVisibility
    }

    private fun handleRoomAliasesEvent(realm: Realm, event: CurrentStateEventEntity, params: CreateRoomParams): CreateRoomParams = params.apply {
        val content = getEventContent<RoomAliasesContent>(realm, event.eventId) ?: return@apply
        roomAliasName = content.aliases.firstOrNull()?.substringAfter("#")?.substringBefore(":")
    }

    private fun handleRoomAvatarEvent(realm: Realm, event: CurrentStateEventEntity, params: CreateRoomParams): CreateRoomParams = params.apply {
        val content = getEventContent<RoomAvatarContent>(realm, event.eventId) ?: return@apply
        avatarUri = content.avatarUrl?.toUri()
    }

    private fun handleRoomCanonicalAliasEvent(realm: Realm, event: CurrentStateEventEntity, params: CreateRoomParams): CreateRoomParams = params.apply {
        val content = getEventContent<RoomCanonicalAliasContent>(realm, event.eventId) ?: return@apply
        roomAliasName = content.canonicalAlias?.substringAfter("#")?.substringBefore(":")
    }

    private fun handleRoomGuestAccessEvent(realm: Realm, event: CurrentStateEventEntity, params: CreateRoomParams): CreateRoomParams = params.apply {
        val content = getEventContent<RoomGuestAccessContent>(realm, event.eventId) ?: return@apply
        guestAccess = content.guestAccess
    }

    private fun handleRoomEncryptionEvent(params: CreateRoomParams): CreateRoomParams = params.apply {
        // Having an encryption event means the room is encrypted, so just enable it again
        enableEncryption()
    }

    private fun handleRoomPowerRoomLevelsEvent(realm: Realm, event: CurrentStateEventEntity, params: CreateRoomParams): CreateRoomParams = params.apply {
        val content = getEventContent<PowerLevelsContent>(realm, event.eventId) ?: return@apply
        powerLevelContentOverride = content
    }

    private fun handleRoomNameEvent(realm: Realm, event: CurrentStateEventEntity, params: CreateRoomParams): CreateRoomParams = params.apply {
        val content = getEventContent<RoomNameContent>(realm, event.eventId) ?: return@apply
        name = content.name
    }

    private fun handleRoomTopicEvent(realm: Realm, event: CurrentStateEventEntity, params: CreateRoomParams): CreateRoomParams = params.apply {
        val content = getEventContent<RoomTopicContent>(realm, event.eventId) ?: return@apply
        topic = content.topic
    }

    private fun handleRoomThirdPartyInviteEvent(event: CurrentStateEventEntity, params: CreateRoomParams): CreateRoomParams = params.apply {
        when {
            event.stateKey.isEmail() -> invite3pids.add(ThreePid.Email(event.stateKey))
            event.stateKey.isMsisdn() -> invite3pids.add(ThreePid.Msisdn(event.stateKey))
        }
    }

    private fun handleRoomJoinRulesEvent(realm: Realm, event: CurrentStateEventEntity, params: CreateRoomParams): CreateRoomParams = params.apply {
        val content = getEventContent<RoomJoinRulesContent>(realm, event.eventId) ?: return@apply
        preset = when {
            // If preset has already been set for direct chat, keep it
            preset == CreateRoomPreset.PRESET_TRUSTED_PRIVATE_CHAT -> CreateRoomPreset.PRESET_TRUSTED_PRIVATE_CHAT
            content.joinRules == RoomJoinRules.PUBLIC -> CreateRoomPreset.PRESET_PUBLIC_CHAT
            content.joinRules == RoomJoinRules.INVITE -> CreateRoomPreset.PRESET_PRIVATE_CHAT
            else -> null
        }
    }

    /* ==========================================================================================
     * Helper methods
     * ========================================================================================== */

    private inline fun <reified T> getEventContent(realm: Realm, eventId: String): T? {
        return EventEntity.where(realm, eventId).findFirst()?.asDomain()?.getClearContent().toModel<T>()
    }

    /**
     * Check if a CharSequence is an email.
     */
    private fun CharSequence.isEmail() = Patterns.EMAIL_ADDRESS.matcher(this).matches()

    /**
     * Check if a CharSequence is a phone number.
     */
    private fun CharSequence.isMsisdn(): Boolean {
        return try {
            PhoneNumberUtil.getInstance().parse(ensurePrefix("+"), null)
            true
        } catch (e: NumberParseException) {
            false
        }
    }
}
