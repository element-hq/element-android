/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.create

import android.net.Uri
import im.vector.app.core.platform.ViewModelTask
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.raw.wellknown.isE2EByDefault
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesAllowEntry
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import org.matrix.android.sdk.api.session.room.model.create.RestrictedRoomPreset
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import org.matrix.android.sdk.api.session.space.CreateSpaceParams
import timber.log.Timber
import javax.inject.Inject

sealed class CreateSpaceTaskResult {

    data class Success(val spaceId: String, val childIds: List<String>) : CreateSpaceTaskResult()

    data class PartialSuccess(val spaceId: String, val childIds: List<String>, val failedRooms: Map<String, Throwable>) : CreateSpaceTaskResult()

    class FailedToCreateSpace(val failure: Throwable) : CreateSpaceTaskResult()
}

data class CreateSpaceTaskParams(
        val spaceName: String,
        val spaceTopic: String?,
        val spaceAvatar: Uri? = null,
        val spaceAlias: String? = null,
        val isPublic: Boolean,
        val defaultRooms: List<String> = emptyList(),
        val defaultEmailToInvite: List<String> = emptyList()
)

class CreateSpaceViewModelTask @Inject constructor(
        private val session: Session,
        private val vectorPreferences: VectorPreferences,
        private val rawService: RawService
) : ViewModelTask<CreateSpaceTaskParams, CreateSpaceTaskResult> {

    override suspend fun execute(params: CreateSpaceTaskParams): CreateSpaceTaskResult {
        val spaceID = try {
            session.spaceService().createSpace(CreateSpaceParams().apply {
                this.name = params.spaceName
                this.topic = params.spaceTopic
                this.avatarUri = params.spaceAvatar
                if (params.isPublic) {
                    this.roomAliasName = params.spaceAlias
                    this.powerLevelContentOverride = (powerLevelContentOverride ?: PowerLevelsContent()).copy(
                            invite = Role.Default.value
                    )
                    this.preset = CreateRoomPreset.PRESET_PUBLIC_CHAT
                    this.historyVisibility = RoomHistoryVisibility.WORLD_READABLE
                    this.guestAccess = GuestAccess.CanJoin
                } else {
                    this.preset = CreateRoomPreset.PRESET_PRIVATE_CHAT
                    visibility = RoomDirectoryVisibility.PRIVATE
                    this.invite3pids.addAll(
                            params.defaultEmailToInvite.map {
                                ThreePid.Email(it)
                            }
                    )
                    this.powerLevelContentOverride = (powerLevelContentOverride ?: PowerLevelsContent()).copy(
                            invite = Role.Moderator.value
                    )
                }
            })
        } catch (failure: Throwable) {
            return CreateSpaceTaskResult.FailedToCreateSpace(failure)
        }

        val createdSpace = session.spaceService().getSpace(spaceID)

        val childErrors = mutableMapOf<String, Throwable>()
        val childIds = mutableListOf<String>()

        val e2eByDefault = tryOrNull {
            rawService.getElementWellknown(session.sessionParams)
                    ?.isE2EByDefault()
                    ?: true
        } ?: true

        params.defaultRooms
                .filter { it.isNotBlank() }
                .forEach { roomName ->
                    try {
                        val roomId = try {
                            if (params.isPublic) {
                                session.roomService().createRoom(
                                        CreateRoomParams().apply {
                                            this.name = roomName
                                            this.preset = CreateRoomPreset.PRESET_PUBLIC_CHAT
                                        }
                                )
                            } else {
                                val homeServerCapabilities = session
                                        .homeServerCapabilitiesService()
                                        .getHomeServerCapabilities()
                                val restrictedSupport = homeServerCapabilities
                                        .isFeatureSupported(HomeServerCapabilities.ROOM_CAP_RESTRICTED)

                                val createRestricted = restrictedSupport == HomeServerCapabilities.RoomCapabilitySupport.SUPPORTED
                                if (createRestricted) {
                                    session.roomService().createRoom(CreateRoomParams().apply {
                                        this.name = roomName
                                        this.featurePreset = RestrictedRoomPreset(
                                                homeServerCapabilities,
                                                listOf(
                                                        RoomJoinRulesAllowEntry.restrictedToRoom(spaceID)
                                                )
                                        )
                                        if (e2eByDefault) {
                                            this.enableEncryption()
                                        }
                                    })
                                } else {
                                    session.roomService().createRoom(CreateRoomParams().apply {
                                        this.name = roomName
                                        visibility = RoomDirectoryVisibility.PRIVATE
                                        this.preset = CreateRoomPreset.PRESET_PRIVATE_CHAT
                                        if (e2eByDefault) {
                                            this.enableEncryption()
                                        }
                                    })
                                }
                            }
                        } catch (timeout: CreateRoomFailure.CreatedWithTimeout) {
                            // we ignore that?
                            timeout.roomID
                        }
                        val via = session.sessionParams.homeServerHost?.let { listOf(it) } ?: emptyList()
                        createdSpace!!.addChildren(roomId, via, null, suggested = true)
                        // set canonical
                        session.spaceService().setSpaceParent(
                                roomId,
                                createdSpace.spaceId,
                                true,
                                via
                        )
                        childIds.add(roomId)
                    } catch (failure: Throwable) {
                        Timber.d("Space: Failed to create child room in $spaceID")
                        childErrors[roomName] = failure
                    }
                }

        return if (childErrors.isEmpty()) {
            CreateSpaceTaskResult.Success(spaceID, childIds)
        } else {
            CreateSpaceTaskResult.PartialSuccess(spaceID, childIds, childErrors)
        }
    }
}
