/*
 * Copyright 2020 New Vector Ltd
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
 *
 */

package im.vector.matrix.android.internal.session.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.identity.ThreePid
import im.vector.matrix.android.api.session.profile.ProfileService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.CancelableBag
import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.database.model.UserThreePidEntity
import im.vector.matrix.android.internal.di.SessionId
import im.vector.matrix.android.internal.di.WorkManagerProvider
import im.vector.matrix.android.internal.session.content.UploadAvatarWorker
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.CancelableWork
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import io.realm.kotlin.where
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val UPLOAD_AVATAR_WORK = "UPLOAD_AVATAR_WORK"

internal class DefaultProfileService @Inject constructor(private val taskExecutor: TaskExecutor,
                                                         private val monarchy: Monarchy,
                                                         @SessionId private val sessionId: String,
                                                         private val workManagerProvider: WorkManagerProvider,
                                                         private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                         private val refreshUserThreePidsTask: RefreshUserThreePidsTask,
                                                         private val getProfileInfoTask: GetProfileInfoTask,
                                                         private val setDisplayNameTask: SetDisplayNameTask,
                                                         private val setAvatarUrlTask: SetAvatarUrlTask) : ProfileService {

    override fun getDisplayName(userId: String, matrixCallback: MatrixCallback<Optional<String>>): Cancelable {
        val params = GetProfileInfoTask.Params(userId)
        return getProfileInfoTask
                .configureWith(params) {
                    this.callback = object : MatrixCallback<JsonDict> {
                        override fun onSuccess(data: JsonDict) {
                            val displayName = data[ProfileService.DISPLAY_NAME_KEY] as? String
                            matrixCallback.onSuccess(Optional.from(displayName))
                        }

                        override fun onFailure(failure: Throwable) {
                            matrixCallback.onFailure(failure)
                        }
                    }
                }
                .executeBy(taskExecutor)
    }

    override fun setDisplayName(userId: String, newDisplayName: String, matrixCallback: MatrixCallback<Unit>): Cancelable {
        return setDisplayNameTask
                .configureWith(SetDisplayNameTask.Params(userId = userId, newDisplayName = newDisplayName)) {
                    callback = matrixCallback
                }
                .executeBy(taskExecutor)
    }

    override fun updateAvatar(userId: String, newAvatarUri: Uri, fileName: String, matrixCallback: MatrixCallback<Unit>): Cancelable {
        val cancelableBag = CancelableBag()
        val workerParams = UploadAvatarWorker.Params(sessionId, newAvatarUri, fileName)
        val workerData = WorkerParamsFactory.toData(workerParams)

        val uploadAvatarWork = workManagerProvider.matrixOneTimeWorkRequestBuilder<UploadAvatarWorker>()
                .setConstraints(WorkManagerProvider.workConstraints)
                .setInputData(workerData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()

        workManagerProvider.workManager
                .beginUniqueWork("${userId}_$UPLOAD_AVATAR_WORK", ExistingWorkPolicy.REPLACE, uploadAvatarWork)
                .enqueue()

        cancelableBag.add(CancelableWork(workManagerProvider.workManager, uploadAvatarWork.id))

        taskExecutor.executorScope.launch(coroutineDispatchers.main) {
            workManagerProvider.workManager.getWorkInfoByIdLiveData(uploadAvatarWork.id)
                    .observeForever { info ->
                        if (info != null && info.state.isFinished) {
                            val result = WorkerParamsFactory.fromData<UploadAvatarWorker.OutputParams>(info.outputData)
                            cancelableBag.add(
                                    setAvatarUrlTask
                                            .configureWith(SetAvatarUrlTask.Params(userId = userId, newAvatarUrl = result?.imageUrl!!)) {
                                                callback = matrixCallback
                                            }
                                            .executeBy(taskExecutor)
                            )
                        }
                    }
        }
        return cancelableBag
    }

    override fun getAvatarUrl(userId: String, matrixCallback: MatrixCallback<Optional<String>>): Cancelable {
        val params = GetProfileInfoTask.Params(userId)
        return getProfileInfoTask
                .configureWith(params) {
                    this.callback = object : MatrixCallback<JsonDict> {
                        override fun onSuccess(data: JsonDict) {
                            val avatarUrl = data[ProfileService.AVATAR_URL_KEY] as? String
                            matrixCallback.onSuccess(Optional.from(avatarUrl))
                        }

                        override fun onFailure(failure: Throwable) {
                            matrixCallback.onFailure(failure)
                        }
                    }
                }
                .executeBy(taskExecutor)
    }

    override fun getProfile(userId: String, matrixCallback: MatrixCallback<JsonDict>): Cancelable {
        val params = GetProfileInfoTask.Params(userId)
        return getProfileInfoTask
                .configureWith(params) {
                    this.callback = matrixCallback
                }
                .executeBy(taskExecutor)
    }

    override fun getThreePids(): List<ThreePid> {
        return monarchy.fetchAllMappedSync(
                { it.where<UserThreePidEntity>() },
                { it.asDomain() }
        )
    }

    override fun getThreePidsLive(refreshData: Boolean): LiveData<List<ThreePid>> {
        if (refreshData) {
            // Force a refresh of the values
            refreshUserThreePidsTask
                    .configureWith()
                    .executeBy(taskExecutor)
        }

        return monarchy.findAllMappedWithChanges(
                { it.where<UserThreePidEntity>() },
                { it.asDomain() }
        )
    }
}

private fun UserThreePidEntity.asDomain(): ThreePid {
    return when (medium) {
        ThirdPartyIdentifier.MEDIUM_EMAIL  -> ThreePid.Email(address)
        ThirdPartyIdentifier.MEDIUM_MSISDN -> ThreePid.Msisdn(address)
        else                               -> error("Invalid medium type")
    }
}
