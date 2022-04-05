/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.summary

import io.realm.Realm
import io.realm.kotlin.createObject
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.accountdata.RoomAccountDataTypes
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomAliasesContent
import org.matrix.android.sdk.api.session.room.model.RoomCanonicalAliasContent
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesContent
import org.matrix.android.sdk.api.session.room.model.RoomNameContent
import org.matrix.android.sdk.api.session.room.model.RoomTopicContent
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.model.VersioningState
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.sync.model.RoomSyncSummary
import org.matrix.android.sdk.api.session.sync.model.RoomSyncUnreadNotifications
import org.matrix.android.sdk.internal.crypto.EventDecryptor
import org.matrix.android.sdk.internal.crypto.crosssigning.DefaultCrossSigningService
import org.matrix.android.sdk.internal.crypto.model.event.EncryptionEventContent
import org.matrix.android.sdk.internal.database.mapper.ContentMapper
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.GroupSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.SpaceChildSummaryEntity
import org.matrix.android.sdk.internal.database.model.SpaceParentSummaryEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.findAllInRoomWithSendStates
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.getOrNull
import org.matrix.android.sdk.internal.database.query.isEventRead
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.extensions.clearWith
import org.matrix.android.sdk.internal.query.process
import org.matrix.android.sdk.internal.session.room.RoomAvatarResolver
import org.matrix.android.sdk.internal.session.room.accountdata.RoomAccountDataDataSource
import org.matrix.android.sdk.internal.session.room.membership.RoomDisplayNameResolver
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberHelper
import org.matrix.android.sdk.internal.session.room.relationship.RoomChildRelationInfo
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

internal class RoomSummaryUpdater @Inject constructor(
        @UserId private val userId: String,
        private val roomDisplayNameResolver: RoomDisplayNameResolver,
        private val roomAvatarResolver: RoomAvatarResolver,
        private val eventDecryptor: EventDecryptor,
        private val crossSigningService: DefaultCrossSigningService,
        private val roomAccountDataDataSource: RoomAccountDataDataSource
) {

    fun refreshLatestPreviewContent(realm: Realm, roomId: String) {
        val roomSummaryEntity = RoomSummaryEntity.getOrNull(realm, roomId)
        if (roomSummaryEntity != null) {
            val latestPreviewableEvent = RoomSummaryEventsHelper.getLatestPreviewableEvent(realm, roomId)
            latestPreviewableEvent?.attemptToDecrypt()
        }
    }

    fun update(realm: Realm,
               roomId: String,
               membership: Membership? = null,
               roomSummary: RoomSyncSummary? = null,
               unreadNotifications: RoomSyncUnreadNotifications? = null,
               updateMembers: Boolean = false,
               inviterId: String? = null) {
        val roomSummaryEntity = RoomSummaryEntity.getOrCreate(realm, roomId)
        if (roomSummary != null) {
            if (roomSummary.heroes.isNotEmpty()) {
                roomSummaryEntity.heroes.clear()
                roomSummaryEntity.heroes.addAll(roomSummary.heroes)
            }
            if (roomSummary.invitedMembersCount != null) {
                roomSummaryEntity.invitedMembersCount = roomSummary.invitedMembersCount
            }
            if (roomSummary.joinedMembersCount != null) {
                roomSummaryEntity.joinedMembersCount = roomSummary.joinedMembersCount
            }
        }
        roomSummaryEntity.highlightCount = unreadNotifications?.highlightCount ?: 0
        roomSummaryEntity.notificationCount = unreadNotifications?.notificationCount ?: 0

        if (membership != null) {
            roomSummaryEntity.membership = membership
        }

        // Hard to filter from the app now we use PagedList...
        roomSummaryEntity.isHiddenFromUser = roomSummaryEntity.versioningState == VersioningState.UPGRADED_ROOM_JOINED ||
                roomAccountDataDataSource.getAccountDataEvent(roomId, RoomAccountDataTypes.EVENT_TYPE_VIRTUAL_ROOM) != null

        val lastNameEvent = CurrentStateEventEntity.getOrNull(realm, roomId, type = EventType.STATE_ROOM_NAME, stateKey = "")?.root
        val lastTopicEvent = CurrentStateEventEntity.getOrNull(realm, roomId, type = EventType.STATE_ROOM_TOPIC, stateKey = "")?.root
        val lastCanonicalAliasEvent = CurrentStateEventEntity.getOrNull(realm, roomId, type = EventType.STATE_ROOM_CANONICAL_ALIAS, stateKey = "")?.root
        val lastAliasesEvent = CurrentStateEventEntity.getOrNull(realm, roomId, type = EventType.STATE_ROOM_ALIASES, stateKey = "")?.root
        val roomCreateEvent = CurrentStateEventEntity.getOrNull(realm, roomId, type = EventType.STATE_ROOM_CREATE, stateKey = "")?.root
        val joinRulesEvent = CurrentStateEventEntity.getOrNull(realm, roomId, type = EventType.STATE_ROOM_JOIN_RULES, stateKey = "")?.root

        val roomType = ContentMapper.map(roomCreateEvent?.content).toModel<RoomCreateContent>()?.type
        roomSummaryEntity.roomType = roomType
        Timber.v("## Space: Updating summary room [$roomId] roomType: [$roomType]")

        val encryptionEvent = CurrentStateEventEntity.getOrNull(realm, roomId, type = EventType.STATE_ROOM_ENCRYPTION, stateKey = "")?.root
        Timber.d("## CRYPTO: currentEncryptionEvent is $encryptionEvent")

        val latestPreviewableEvent = RoomSummaryEventsHelper.getLatestPreviewableEvent(realm, roomId)

        val lastActivityFromEvent = latestPreviewableEvent?.root?.originServerTs
        if (lastActivityFromEvent != null) {
            roomSummaryEntity.lastActivityTime = lastActivityFromEvent
            latestPreviewableEvent.attemptToDecrypt()
        }

        roomSummaryEntity.hasUnreadMessages = roomSummaryEntity.notificationCount > 0 ||
                // avoid this call if we are sure there are unread events
                latestPreviewableEvent?.let { !isEventRead(realm.configuration, userId, roomId, it.eventId) } ?: false

        roomSummaryEntity.setDisplayName(roomDisplayNameResolver.resolve(realm, roomId))
        roomSummaryEntity.avatarUrl = roomAvatarResolver.resolve(realm, roomId)
        roomSummaryEntity.name = ContentMapper.map(lastNameEvent?.content).toModel<RoomNameContent>()?.name
        roomSummaryEntity.topic = ContentMapper.map(lastTopicEvent?.content).toModel<RoomTopicContent>()?.topic
        roomSummaryEntity.joinRules = ContentMapper.map(joinRulesEvent?.content).toModel<RoomJoinRulesContent>()?.joinRules
        roomSummaryEntity.latestPreviewableEvent = latestPreviewableEvent
        roomSummaryEntity.canonicalAlias = ContentMapper.map(lastCanonicalAliasEvent?.content).toModel<RoomCanonicalAliasContent>()
                ?.canonicalAlias

        val roomAliases = ContentMapper.map(lastAliasesEvent?.content).toModel<RoomAliasesContent>()?.aliases
                .orEmpty()
        roomSummaryEntity.updateAliases(roomAliases)
        roomSummaryEntity.isEncrypted = encryptionEvent != null

        roomSummaryEntity.e2eAlgorithm = ContentMapper.map(encryptionEvent?.content)
                ?.toModel<EncryptionEventContent>()
                ?.algorithm

        roomSummaryEntity.encryptionEventTs = encryptionEvent?.originServerTs

        if (roomSummaryEntity.membership == Membership.INVITE && inviterId != null) {
            roomSummaryEntity.inviterId = inviterId
        } else if (roomSummaryEntity.membership != Membership.INVITE) {
            roomSummaryEntity.inviterId = null
        }
        roomSummaryEntity.updateHasFailedSending()

        if (updateMembers) {
            val otherRoomMembers = RoomMemberHelper(realm, roomId)
                    .queryActiveRoomMembersEvent()
                    .notEqualTo(RoomMemberSummaryEntityFields.USER_ID, userId)
                    .findAll()
                    .map { it.userId }

            roomSummaryEntity.otherMemberIds.clear()
            roomSummaryEntity.otherMemberIds.addAll(otherRoomMembers)
            if (roomSummaryEntity.isEncrypted && otherRoomMembers.isNotEmpty()) {
                // mmm maybe we could only refresh shield instead of checking trust also?
                crossSigningService.onUsersDeviceUpdate(otherRoomMembers)
            }
        }
    }

    private fun TimelineEventEntity.attemptToDecrypt() {
        when (val root = this.root) {
            null -> {
                Timber.v("Decryption skipped due to missing root event $eventId")
            }
            else -> {
                if (root.type == EventType.ENCRYPTED && root.decryptionResultJson == null) {
                    Timber.v("Should decrypt $eventId")
                    tryOrNull {
                        runBlocking { eventDecryptor.decryptEvent(root.asDomain(), "") }
                    }?.let { root.setDecryptionResult(it) }
                }
            }
        }
    }

    private fun RoomSummaryEntity.updateHasFailedSending() {
        hasFailedSending = TimelineEventEntity.findAllInRoomWithSendStates(realm, roomId, SendState.HAS_FAILED_STATES).isNotEmpty()
    }

    fun updateSendingInformation(realm: Realm, roomId: String) {
        val roomSummaryEntity = RoomSummaryEntity.getOrCreate(realm, roomId)
        roomSummaryEntity.updateHasFailedSending()
        roomSummaryEntity.latestPreviewableEvent = RoomSummaryEventsHelper.getLatestPreviewableEvent(realm, roomId)
    }

    /**
     * Should be called at the end of the room sync, to check and validate all parent/child relations
     */
    fun validateSpaceRelationship(realm: Realm) {
        measureTimeMillis {
            val lookupMap = realm.where(RoomSummaryEntity::class.java)
                    .process(RoomSummaryEntityFields.MEMBERSHIP_STR, Membership.activeMemberships())
                    // we order by roomID to be consistent when breaking parent/child cycles
                    .sort(RoomSummaryEntityFields.ROOM_ID)
                    .findAll().map {
                        it.flattenParentIds = null
                        it to emptyList<RoomSummaryEntity>().toMutableSet()
                    }
                    .toMap()

            // First handle child relations
            lookupMap.keys.asSequence()
                    .filter { it.roomType == RoomType.SPACE }
                    .forEach { lookedUp ->
                        // get childrens

                        lookedUp.children.clearWith { it.deleteFromRealm() }

                        RoomChildRelationInfo(realm, lookedUp.roomId).getDirectChildrenDescriptions().forEach { child ->

                            lookedUp.children.add(
                                    realm.createObject<SpaceChildSummaryEntity>().apply {
                                        this.childRoomId = child.roomId
                                        this.childSummaryEntity = RoomSummaryEntity.where(realm, child.roomId).findFirst()
                                        this.order = child.order
//                                    this.autoJoin = child.autoJoin
                                        this.viaServers.addAll(child.viaServers)
                                    }
                            )

                            RoomSummaryEntity.where(realm, child.roomId)
                                    .process(RoomSummaryEntityFields.MEMBERSHIP_STR, Membership.activeMemberships())
                                    .findFirst()
                                    ?.let { childSum ->
                                        lookupMap.entries.firstOrNull { it.key.roomId == lookedUp.roomId }?.let { entry ->
                                            if (entry.value.none { it.roomId == childSum.roomId }) {
                                                // add looked up as a parent
                                                entry.value.add(childSum)
                                            }
                                        }
                                    }
                        }
                    }

            // Now let's check parent relations

            lookupMap.keys
                    .forEach { lookedUp ->
                        lookedUp.parents.clearWith { it.deleteFromRealm() }
                        // can we check parent relations here??
                        /**
                         * rooms can claim parents via the m.space.parent state event.
                         * canonical determines whether this is the main parent for the space.
                         *
                         * To avoid abuse where a room admin falsely claims that a room is part of a space that it should not be,
                         * clients could ignore such m.space.parent events unless either
                         * (a) there is a corresponding m.space.child event in the claimed parent, or
                         * (b) the sender of the m.space.child event has a sufficient power-level to send such an m.space.child event in the parent.
                         * (It is not necessarily required that that user currently be a member of the parent room -
                         * only the m.room.power_levels event is inspected.)
                         * [Checking the power-level rather than requiring an actual m.space.child event in the parent allows for "secret" rooms (see below).]
                         */
                        RoomChildRelationInfo(realm, lookedUp.roomId).getParentDescriptions()
                                .map { parentInfo ->
                                    // Is it a valid parent relation?
                                    // Check if it's a child of the parent?
                                    val isValidRelation: Boolean
                                    val parent = lookupMap.firstNotNullOfOrNull { if (it.key.roomId == parentInfo.roomId) it.value else null }
                                    if (parent?.firstOrNull { it.roomId == lookedUp.roomId } != null) {
                                        // there is a corresponding m.space.child event in the claimed parent
                                        isValidRelation = true
                                    } else {
                                        // check if sender can post child relation in parent?
                                        val senderId = parentInfo.stateEventSender
                                        val parentRoomId = parentInfo.roomId
                                        val powerLevelsHelper = CurrentStateEventEntity
                                                .getOrNull(realm, parentRoomId, "", EventType.STATE_ROOM_POWER_LEVELS)
                                                ?.root
                                                ?.let { ContentMapper.map(it.content).toModel<PowerLevelsContent>() }
                                                ?.let { PowerLevelsHelper(it) }

                                        isValidRelation = powerLevelsHelper?.isUserAllowedToSend(senderId, true, EventType.STATE_SPACE_CHILD) ?: false
                                    }

                                    if (isValidRelation) {
                                        lookedUp.parents.add(
                                                realm.createObject<SpaceParentSummaryEntity>().apply {
                                                    this.parentRoomId = parentInfo.roomId
                                                    this.parentSummaryEntity = RoomSummaryEntity.where(realm, parentInfo.roomId).findFirst()
                                                    this.canonical = parentInfo.canonical
                                                    this.viaServers.addAll(parentInfo.viaServers)
                                                }
                                        )

                                        RoomSummaryEntity.where(realm, parentInfo.roomId)
                                                .process(RoomSummaryEntityFields.MEMBERSHIP_STR, Membership.activeMemberships())
                                                .findFirst()
                                                ?.let { parentSum ->
                                                    if (lookupMap[parentSum]?.none { it.roomId == lookedUp.roomId }.orFalse()) {
                                                        // add lookedup as a parent
                                                        lookupMap[parentSum]?.add(lookedUp)
                                                    }
                                                }
                                    }
                                }
                    }

            // Simple algorithm to break cycles
            // Need more work to decide how to break, probably need to be as consistent as possible
            // and also find best way to root the tree

            val graph = Graph()
            lookupMap
                    // focus only on joined spaces, as room are just leaf
                    .filter { it.key.roomType == RoomType.SPACE && it.key.membership == Membership.JOIN }
                    .forEach { (sum, children) ->
                        graph.getOrCreateNode(sum.roomId)
                        children.forEach {
                            graph.addEdge(it.roomId, sum.roomId)
                        }
                    }

            val backEdges = graph.findBackwardEdges()
            Timber.v("## SPACES: Cycle detected = ${backEdges.isNotEmpty()}")

            // break cycles
            backEdges.forEach { edge ->
                lookupMap.entries.find { it.key.roomId == edge.source.name }?.let {
                    it.value.removeAll { it.roomId == edge.destination.name }
                }
            }

            val acyclicGraph = graph.withoutEdges(backEdges)
//            Timber.v("## SPACES: acyclicGraph $acyclicGraph")
            val flattenSpaceParents = acyclicGraph.flattenDestination().map {
                it.key.name to it.value.map { it.name }
            }.toMap()
//            Timber.v("## SPACES: flattenSpaceParents ${flattenSpaceParents.map { it.key.name to it.value.map { it.name } }.joinToString("\n") {
//                it.first + ": [" + it.second.joinToString(",") + "]"
//            }}")

//            Timber.v("## SPACES: lookup map ${lookupMap.map { it.key.name to it.value.map { it.name } }.toMap()}")

            lookupMap.entries
                    .filter { it.key.roomType == RoomType.SPACE && it.key.membership == Membership.JOIN }
                    .forEach { entry ->
                        val parent = RoomSummaryEntity.where(realm, entry.key.roomId).findFirst()
                        if (parent != null) {
//                            Timber.v("## SPACES: check hierarchy of ${parent.name} id ${parent.roomId}")
//                            Timber.v("## SPACES: flat known parents of ${parent.name} are ${flattenSpaceParents[parent.roomId]}")
                            val flattenParentsIds = (flattenSpaceParents[parent.roomId] ?: emptyList()) + listOf(parent.roomId)
//                            Timber.v("## SPACES: flatten known parents of children of ${parent.name} are ${flattenParentsIds}")
                            entry.value.forEach { child ->
                                RoomSummaryEntity.where(realm, child.roomId).findFirst()?.let { childSum ->

//                                    Timber.w("## SPACES: ${childSum.name} is ${childSum.roomId} fc: ${childSum.flattenParentIds}")
//                                    var allParents = childSum.flattenParentIds ?: ""
                                    if (childSum.flattenParentIds == null) childSum.flattenParentIds = ""
                                    flattenParentsIds.forEach {
                                        if (childSum.flattenParentIds?.contains(it) != true) {
                                            childSum.flattenParentIds += "|$it"
                                        }
                                    }
//                                    childSum.flattenParentIds = "$allParents|"

//                                    Timber.v("## SPACES: flatten of ${childSum.name} is ${childSum.flattenParentIds}")
                                }
                            }
                        }
                    }

            // we need also to filter DMs...
            // it's more annoying as based on if the other members belong the space or not
            RoomSummaryEntity.where(realm)
                    .equalTo(RoomSummaryEntityFields.IS_DIRECT, true)
                    .process(RoomSummaryEntityFields.MEMBERSHIP_STR, Membership.activeMemberships())
                    .findAll()
                    .forEach { dmRoom ->
                        val relatedSpaces = lookupMap.keys
                                .filter { it.roomType == RoomType.SPACE }
                                .filter {
                                    dmRoom.otherMemberIds.toList().intersect(it.otherMemberIds.toList()).isNotEmpty()
                                }
                                .map { it.roomId }
                                .distinct()
                        val flattenRelated = mutableListOf<String>().apply {
                            addAll(relatedSpaces)
                            relatedSpaces.map { flattenSpaceParents[it] }.forEach {
                                if (it != null) addAll(it)
                            }
                        }.distinct()
                        if (flattenRelated.isNotEmpty()) {
                            // we keep real m.child/m.parent relations and add the one for common memberships
                            dmRoom.flattenParentIds += "|${flattenRelated.joinToString("|")}|"
                        }
//                        Timber.v("## SPACES: flatten of ${dmRoom.otherMemberIds.joinToString(",")} is ${dmRoom.flattenParentIds}")
                    }

            // Maybe a good place to count the number of notifications for spaces?

            realm.where(RoomSummaryEntity::class.java)
                    .process(RoomSummaryEntityFields.MEMBERSHIP_STR, Membership.activeMemberships())
                    .equalTo(RoomSummaryEntityFields.ROOM_TYPE, RoomType.SPACE)
                    .findAll().forEach { space ->
                        // get all children
                        var highlightCount = 0
                        var notificationCount = 0
                        realm.where(RoomSummaryEntity::class.java)
                                .process(RoomSummaryEntityFields.MEMBERSHIP_STR, listOf(Membership.JOIN))
                                .notEqualTo(RoomSummaryEntityFields.ROOM_TYPE, RoomType.SPACE)
                                .contains(RoomSummaryEntityFields.FLATTEN_PARENT_IDS, space.roomId)
                                .findAll().forEach {
                                    highlightCount += it.highlightCount
                                    notificationCount += it.notificationCount
                                }

                        space.highlightCount = highlightCount
                        space.notificationCount = notificationCount
                    }
            // xxx invites??

            // LEGACY GROUPS
            // lets mark rooms that belongs to groups
            val existingGroups = GroupSummaryEntity.where(realm).findAll()

            // For rooms
            realm.where(RoomSummaryEntity::class.java)
                    .process(RoomSummaryEntityFields.MEMBERSHIP_STR, Membership.activeMemberships())
                    .equalTo(RoomSummaryEntityFields.IS_DIRECT, false)
                    .findAll().forEach { room ->
                        val belongsTo = existingGroups.filter { it.roomIds.contains(room.roomId) }
                        room.groupIds = if (belongsTo.isEmpty()) {
                            null
                        } else {
                            "|${belongsTo.joinToString("|")}|"
                        }
                    }

            // For DMS
            realm.where(RoomSummaryEntity::class.java)
                    .process(RoomSummaryEntityFields.MEMBERSHIP_STR, Membership.activeMemberships())
                    .equalTo(RoomSummaryEntityFields.IS_DIRECT, true)
                    .findAll().forEach { room ->
                        val belongsTo = existingGroups.filter {
                            it.userIds.intersect(room.otherMemberIds).isNotEmpty()
                        }
                        room.groupIds = if (belongsTo.isEmpty()) {
                            null
                        } else {
                            "|${belongsTo.joinToString("|")}|"
                        }
                    }
        }.also {
            Timber.v("## SPACES: Finish checking room hierarchy in $it ms")
        }
    }

//    private fun isValidCanonical() : Boolean {
//
//    }
}
