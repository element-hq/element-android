/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.upgrade

import im.vector.app.core.platform.ViewModelTask
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import timber.log.Timber
import javax.inject.Inject

class UpgradeRoomViewModelTask @Inject constructor(
        val session: Session,
        val stringProvider: StringProvider
) : ViewModelTask<UpgradeRoomViewModelTask.Params, UpgradeRoomViewModelTask.Result> {

    sealed class Result {
        data class Success(val replacementRoomId: String) : Result()
        abstract class Failure(val throwable: Throwable?) : Result()
        object UnknownRoom : Failure(null)
        object NotAllowed : Failure(null)
        class ErrorFailure(throwable: Throwable) : Failure(throwable)
    }

    data class Params(
            val roomId: String,
            val newVersion: String,
            val userIdsToAutoInvite: List<String> = emptyList(),
            val parentSpaceToUpdate: List<String> = emptyList(),
            val progressReporter: ((indeterminate: Boolean, progress: Int, total: Int) -> Unit)? = null
    )

    override suspend fun execute(params: Params): Result {
        params.progressReporter?.invoke(true, 0, 0)

        val room = session.getRoom(params.roomId)
                ?: return Result.UnknownRoom
        if (!room.roomVersionService().userMayUpgradeRoom(session.myUserId)) {
            return Result.NotAllowed
        }

        val updatedRoomId = try {
            room.roomVersionService().upgradeToVersion(params.newVersion)
        } catch (failure: Throwable) {
            return Result.ErrorFailure(failure)
        }

        val totalStep = params.userIdsToAutoInvite.size + params.parentSpaceToUpdate.size
        var currentStep = 0
        params.userIdsToAutoInvite.forEach {
            params.progressReporter?.invoke(false, currentStep, totalStep)
            tryOrNull {
                session.getRoom(updatedRoomId)?.membershipService()?.invite(it)
            }
            currentStep++
        }

        params.parentSpaceToUpdate.forEach { parentId ->
            params.progressReporter?.invoke(false, currentStep, totalStep)
            // we try and silently fail
            try {
                session.getRoom(parentId)?.asSpace()?.let { parentSpace ->
                    val currentInfo = parentSpace.getChildInfo(params.roomId)
                    if (currentInfo != null) {
                        parentSpace.addChildren(
                                roomId = updatedRoomId,
                                viaServers = currentInfo.via,
                                order = currentInfo.order,
//                                autoJoin = currentInfo.autoJoin ?: false,
                                suggested = currentInfo.suggested
                        )
                    }
                }
            } catch (failure: Throwable) {
                Timber.d("## Migrate: Failed to update space parent. cause: ${failure.localizedMessage}")
            } finally {
                currentStep++
            }
        }

        return Result.Success(updatedRoomId)
    }
}
