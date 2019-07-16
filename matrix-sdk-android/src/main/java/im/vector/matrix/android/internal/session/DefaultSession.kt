/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import dagger.Lazy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.pushrules.PushRuleService
import im.vector.matrix.android.api.session.InitialSyncProgressService
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.cache.CacheService
import im.vector.matrix.android.api.session.content.ContentUploadStateTracker
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.file.FileService
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.pushers.PushersService
import im.vector.matrix.android.api.session.room.RoomDirectoryService
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.api.session.signout.SignOutService
import im.vector.matrix.android.api.session.sync.FilterService
import im.vector.matrix.android.api.session.sync.SyncState
import im.vector.matrix.android.api.session.user.UserService
import im.vector.matrix.android.api.util.MatrixCallbackDelegate
import im.vector.matrix.android.internal.crypto.CryptoManager
import im.vector.matrix.android.internal.database.LiveEntityObserver
import im.vector.matrix.android.internal.session.sync.job.SyncThread
import im.vector.matrix.android.internal.session.sync.job.SyncWorker
import im.vector.matrix.android.internal.worker.WorkManagerUtil
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class DefaultSession @Inject constructor(override val sessionParams: SessionParams,
                                                  private val context: Context,
                                                  private val liveEntityObservers: Set<@JvmSuppressWildcards LiveEntityObserver>,
                                                  private val sessionListeners: SessionListeners,
                                                  private val roomService: Lazy<RoomService>,
                                                  private val roomDirectoryService: Lazy<RoomDirectoryService>,
                                                  private val groupService: Lazy<GroupService>,
                                                  private val userService: Lazy<UserService>,
                                                  private val filterService: Lazy<FilterService>,
                                                  private val cacheService: Lazy<CacheService>,
                                                  private val signOutService: Lazy<SignOutService>,
                                                  private val pushRuleService: Lazy<PushRuleService>,
                                                  private val pushersService: Lazy<PushersService>,
                                                  private val cryptoService: Lazy<CryptoManager>,
                                                  private val fileService: Lazy<FileService>,
                                                  private val syncThread: SyncThread,
                                                  private val contentUrlResolver: ContentUrlResolver,
                                                  private val contentUploadProgressTracker: ContentUploadStateTracker,
                                                  private val initialSyncProgressService: Lazy<InitialSyncProgressService>)
    : Session,
        RoomService by roomService.get(),
        RoomDirectoryService by roomDirectoryService.get(),
        GroupService by groupService.get(),
        UserService by userService.get(),
        CryptoService by cryptoService.get(),
        CacheService by cacheService.get(),
        SignOutService by signOutService.get(),
        FilterService by filterService.get(),
        PushRuleService by pushRuleService.get(),
        PushersService by pushersService.get(),
        FileService by fileService.get(),
        InitialSyncProgressService by initialSyncProgressService.get() {

    private var isOpen = false


    @MainThread
    override fun open() {
        assertMainThread()
        assert(!isOpen)
        isOpen = true
        liveEntityObservers.forEach { it.start() }
    }

    override fun requireBackgroundSync() {
        SyncWorker.requireBackgroundSync(context, sessionParams.credentials.userId)
    }

    override fun startAutomaticBackgroundSync(repeatDelay: Long) {
        SyncWorker.automaticallyBackgroundSync(context, sessionParams.credentials.userId, 0, repeatDelay)
    }

    override fun stopAnyBackgroundSync() {
        SyncWorker.stopAnyBackgroundSync(context)
    }

    override fun startSync(fromForeground : Boolean) {
        Timber.i("Starting sync thread")
        assert(isOpen)
        syncThread.setInitialForeground(fromForeground)
        if (!syncThread.isAlive) {
            syncThread.start()
        } else {
            syncThread.restart()
            Timber.w("Attempt to start an already started thread")
        }
    }

    override fun stopSync() {
        assert(isOpen)
        syncThread.kill()
    }

    override fun close() {
        assert(isOpen)
        stopSync()
        liveEntityObservers.forEach { it.dispose() }
        cryptoService.get().close()
        isOpen = false
    }

    override fun syncState(): LiveData<SyncState> {
        return syncThread.liveState()
    }

    @MainThread
    override fun signOut(callback: MatrixCallback<Unit>) {
        Timber.w("SIGN_OUT: start")

        assert(isOpen)
        //Timber.w("SIGN_OUT: kill sync thread")
        //syncThread.kill()

        Timber.w("SIGN_OUT: call webservice")
        return signOutService.get().signOut(object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                Timber.w("SIGN_OUT: call webservice -> SUCCESS: clear cache")

                // Clear the cache
                cacheService.get().clearCache(object : MatrixCallback<Unit> {
                    override fun onSuccess(data: Unit) {
                        Timber.w("SIGN_OUT: clear cache -> SUCCESS: clear crypto cache")
                        cryptoService.get().clearCryptoCache(MatrixCallbackDelegate(callback))

                        WorkManagerUtil.cancelAllWorks(context)
                    }

                    override fun onFailure(failure: Throwable) {
                        // ignore error
                        Timber.e("SIGN_OUT: clear cache -> ERROR: ignoring")
                        onSuccess(Unit)
                    }
                })
            }

            override fun onFailure(failure: Throwable) {
                // Ignore failure
                Timber.e("SIGN_OUT: call webservice -> ERROR: ignoring")
                onSuccess(Unit)
            }
        })
    }

    override fun contentUrlResolver() = contentUrlResolver

    override fun contentUploadProgressTracker() = contentUploadProgressTracker

    override fun addListener(listener: Session.Listener) {
        sessionListeners.addListener(listener)
    }

    override fun removeListener(listener: Session.Listener) {
        sessionListeners.removeListener(listener)
    }

    // Private methods *****************************************************************************

    private fun assertMainThread() {
        if (Looper.getMainLooper().thread !== Thread.currentThread()) {
            throw IllegalStateException("This method can only be called on the main thread!")
        }
    }

}