/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.sync

import okhttp3.ResponseBody
import okio.buffer
import okio.source
import org.matrix.android.sdk.R
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.di.SessionFilesDirectory
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.TimeOutInterceptor
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.network.toFailure
import org.matrix.android.sdk.internal.session.DefaultInitialSyncProgressService
import org.matrix.android.sdk.internal.session.filter.FilterRepository
import org.matrix.android.sdk.internal.session.homeserver.GetHomeServerCapabilitiesTask
import org.matrix.android.sdk.internal.session.reportSubtask
import org.matrix.android.sdk.internal.session.sync.model.LazyRoomSync
import org.matrix.android.sdk.internal.session.sync.model.LazyRoomSyncJsonAdapter
import org.matrix.android.sdk.internal.session.sync.model.SyncResponse
import org.matrix.android.sdk.internal.session.user.UserStore
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.logDuration
import retrofit2.Response
import retrofit2.awaitResponse
import timber.log.Timber
import java.io.File
import java.net.SocketTimeoutException
import javax.inject.Inject

internal interface SyncTask : Task<SyncTask.Params, Unit> {

    data class Params(
            val timeout: Long,
            val presence: SyncPresence?
    )
}

internal class DefaultSyncTask @Inject constructor(
        private val syncAPI: SyncAPI,
        @UserId private val userId: String,
        private val filterRepository: FilterRepository,
        private val syncResponseHandler: SyncResponseHandler,
        private val initialSyncProgressService: DefaultInitialSyncProgressService,
        private val syncTokenStore: SyncTokenStore,
        private val getHomeServerCapabilitiesTask: GetHomeServerCapabilitiesTask,
        private val userStore: UserStore,
        private val syncTaskSequencer: SyncTaskSequencer,
        private val globalErrorReceiver: GlobalErrorReceiver,
        @SessionFilesDirectory
        private val fileDirectory: File
) : SyncTask {

    private val workingDir = File(fileDirectory, "is")
    private val initialSyncStatusRepository: InitialSyncStatusRepository = FileInitialSyncStatusRepository(workingDir)

    override suspend fun execute(params: SyncTask.Params) = syncTaskSequencer.post {
        doSync(params)
    }

    private suspend fun doSync(params: SyncTask.Params) {
        Timber.v("Sync task started on Thread: ${Thread.currentThread().name}")

        val requestParams = HashMap<String, String>()
        var timeout = 0L
        val token = syncTokenStore.getLastToken()
        if (token != null) {
            requestParams["since"] = token
            timeout = params.timeout
        }
        requestParams["timeout"] = timeout.toString()
        requestParams["filter"] = filterRepository.getFilter()
        params.presence?.let { requestParams["set_presence"] = it.value }

        val isInitialSync = token == null
        if (isInitialSync) {
            // We might want to get the user information in parallel too
            userStore.createOrUpdate(userId)
            initialSyncProgressService.endAll()
            initialSyncProgressService.startTask(R.string.initial_sync_start_importing_account, 100)
        }
        // Maybe refresh the home server capabilities data we know
        getHomeServerCapabilitiesTask.execute(GetHomeServerCapabilitiesTask.Params(forceRefresh = false))

        val readTimeOut = (params.timeout + TIMEOUT_MARGIN).coerceAtLeast(TimeOutInterceptor.DEFAULT_LONG_TIMEOUT)

        if (isInitialSync) {
            logDuration("INIT_SYNC strategy: $initialSyncStrategy") {
                if (initialSyncStrategy is InitialSyncStrategy.Optimized) {
                    safeInitialSync(requestParams)
                } else {
                    val syncResponse = logDuration("INIT_SYNC Request") {
                        executeRequest<SyncResponse>(globalErrorReceiver) {
                            apiCall = syncAPI.sync(
                                    params = requestParams,
                                    readTimeOut = readTimeOut
                            )
                        }
                    }

                    logDuration("INIT_SYNC Database insertion") {
                        syncResponseHandler.handleResponse(syncResponse, token)
                    }
                }
            }
            initialSyncProgressService.endAll()
        } else {
            val syncResponse = executeRequest<SyncResponse>(globalErrorReceiver) {
                apiCall = syncAPI.sync(
                        params = requestParams,
                        readTimeOut = readTimeOut
                )
            }
            syncResponseHandler.handleResponse(syncResponse, token)
        }
        Timber.v("Sync task finished on Thread: ${Thread.currentThread().name}")
    }

    private suspend fun safeInitialSync(requestParams: Map<String, String>) {
        workingDir.mkdirs()
        val workingFile = File(workingDir, "initSync.json")
        val status = initialSyncStatusRepository.getStep()
        if (workingFile.exists() && status >= InitialSyncStatus.STEP_DOWNLOADED) {
            // Go directly to the parse step
            Timber.v("INIT_SYNC file is already here")
        } else {
            initialSyncStatusRepository.setStep(InitialSyncStatus.STEP_DOWNLOADING)
            val syncResponse = logDuration("INIT_SYNC Perform server request") {
                reportSubtask(initialSyncProgressService, R.string.initial_sync_start_server_computing, 0, 0.5f) {
                    getSyncResponse(requestParams, MAX_NUMBER_OF_RETRY_AFTER_TIMEOUT)
                }
            }

            if (syncResponse.isSuccessful) {
                logDuration("INIT_SYNC Download and save to file") {
                    reportSubtask(initialSyncProgressService, R.string.initial_sync_start_downloading, 0, 0.5f) {
                        syncResponse.body()?.byteStream()?.use { inputStream ->
                            workingFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                }
            } else {
                throw syncResponse.toFailure(globalErrorReceiver)
                        .also { Timber.w("INIT_SYNC request failure: $this") }
            }
            initialSyncStatusRepository.setStep(InitialSyncStatus.STEP_DOWNLOADED)
        }
        handleSyncFile(workingFile)

        // Delete all files
        workingDir.deleteRecursively()
    }

    private suspend fun getSyncResponse(requestParams: Map<String, String>, maxNumberOfRetries: Int): Response<ResponseBody> {
        var retry = maxNumberOfRetries
        while (true) {
            retry--
            try {
                return syncAPI.syncStream(
                        params = requestParams
                ).awaitResponse()
            } catch (throwable: Throwable) {
                if (throwable is SocketTimeoutException && retry > 0) {
                    Timber.w("INIT_SYNC timeout retry left: $retry")
                } else {
                    Timber.e(throwable, "INIT_SYNC timeout, no retry left, or other error")
                    throw throwable
                }
            }
        }
    }

    private suspend fun handleSyncFile(workingFile: File) {
        val syncResponseLength = workingFile.length().toInt()

        logDuration("INIT_SYNC handleSyncFile() file size $syncResponseLength bytes") {
            if (syncResponseLength < (initialSyncStrategy as? InitialSyncStrategy.Optimized)?.minSizeToSplit ?: Long.MAX_VALUE) {
                // OK, no need to split just handle as a regular sync response
                Timber.v("INIT_SYNC no need to split")
                handleInitialSyncFile(workingFile)
            } else {
                Timber.v("INIT_SYNC Split into several smaller files")
                // Set file mode
                // TODO This is really ugly, I should improve that
                LazyRoomSyncJsonAdapter.initWith(workingFile)

                handleInitialSyncFile(workingFile)

                // Reset file mode
                LazyRoomSyncJsonAdapter.reset()
            }
        }
    }

    private suspend fun handleInitialSyncFile(workingFile: File) {
        val syncResponse = logDuration("INIT_SYNC Read file and parse") {
            MoshiProvider.providesMoshi().adapter(SyncResponse::class.java)
                    .fromJson(workingFile.source().buffer())!!
        }
        initialSyncStatusRepository.setStep(InitialSyncStatus.STEP_PARSED)

        // Log some stats
        val nbOfJoinedRooms = syncResponse.rooms?.join?.size ?: 0
        val nbOfJoinedRoomsInFile = syncResponse.rooms?.join?.values?.count { it is LazyRoomSync.Stored }
        Timber.v("INIT_SYNC $nbOfJoinedRooms rooms, $nbOfJoinedRoomsInFile stored into files")

        logDuration("INIT_SYNC Database insertion") {
            syncResponseHandler.handleResponse(syncResponse, null)
        }
        initialSyncStatusRepository.setStep(InitialSyncStatus.STEP_SUCCESS)
    }

    companion object {
        private const val MAX_NUMBER_OF_RETRY_AFTER_TIMEOUT = 50

        private const val TIMEOUT_MARGIN: Long = 10_000
    }
}
