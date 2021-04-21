/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.spaces.create

import android.net.Uri
import im.vector.app.core.platform.ViewModelTask
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import org.matrix.android.sdk.internal.util.awaitCallback
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
        val isPublic: Boolean,
        val defaultRooms: List<String> = emptyList()
)

class CreateSpaceViewModelTask @Inject constructor(
        private val session: Session,
        private val stringProvider: StringProvider
) : ViewModelTask<CreateSpaceTaskParams, CreateSpaceTaskResult> {

    override suspend fun execute(params: CreateSpaceTaskParams): CreateSpaceTaskResult {
        val spaceID = try {
            session.spaceService().createSpace(params.spaceName, params.spaceTopic, params.spaceAvatar, params.isPublic)
        } catch (failure: Throwable) {
            return CreateSpaceTaskResult.FailedToCreateSpace(failure)
        }

        val createdSpace = session.spaceService().getSpace(spaceID)

        val childErrors = mutableMapOf<String, Throwable>()
        val childIds = mutableListOf<String>()
        if (params.isPublic) {
            params.defaultRooms
                    .filter { it.isNotBlank() }
                    .forEach { roomName ->
                        try {
                            val roomId = try {
                                awaitCallback<String> {
                                    session.createRoom(CreateRoomParams().apply {
                                        this.name = roomName
                                        this.preset = CreateRoomPreset.PRESET_PUBLIC_CHAT
                                    }, it)
                                }
                            } catch (timeout: CreateRoomFailure.CreatedWithTimeout) {
                                // we ignore that?
                                timeout.roomID
                            }
                            val via = session.sessionParams.homeServerHost?.let { listOf(it) } ?: emptyList()
                            createdSpace!!.addChildren(roomId, via, null, true)
                            childIds.add(roomId)
                        } catch (failure: Throwable) {
                            Timber.d("Failed to create child room in $spaceID")
                            childErrors[roomName] = failure
                        }
                    }
        }

        return if (childErrors.isEmpty()) {
            CreateSpaceTaskResult.Success(spaceID, childIds)
        } else {
            CreateSpaceTaskResult.PartialSuccess(spaceID, childIds, childErrors)
        }
    }
}
