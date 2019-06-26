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
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.pushrules.PushRuleService
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.cache.CacheService
import im.vector.matrix.android.api.session.content.ContentUploadStateTracker
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.crypto.CryptoService
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
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class DefaultSession @Inject constructor(override val sessionParams: SessionParams,
                                                  private val context: Context,
                                                  private val liveEntityObservers: Set<@JvmSuppressWildcards LiveEntityObserver>,
                                                  private val monarchy: Monarchy,
                                                  private val sessionListeners: SessionListeners,
                                                  private val roomService: RoomService,
                                                  private val roomDirectoryService: RoomDirectoryService,
                                                  private val groupService: GroupService,
                                                  private val userService: UserService,
                                                  private val filterService: FilterService,
                                                  private val cacheService: CacheService,
                                                  private val signOutService: SignOutService,
                                                  private val pushRuleService: PushRuleService,
                                                  private val pushersService: PushersService,
                                                  private val cryptoService: CryptoManager,
                                                  private val syncThread: SyncThread,
                                                  private val contentUrlResolver: ContentUrlResolver,
                                                  private val contentUploadProgressTracker: ContentUploadStateTracker)
    : Session,
        RoomService by roomService,
        RoomDirectoryService by roomDirectoryService,
        GroupService by groupService,
        UserService by userService,
        CryptoService by cryptoService,
        CacheService by cacheService,
        SignOutService by signOutService,
        FilterService by filterService,
        PushRuleService by pushRuleService,
        PushersService by pushersService {

    private var isOpen = false


    @MainThread
    override fun open() {
        assertMainThread()
        assert(!isOpen)
        isOpen = true
        if (!monarchy.isMonarchyThreadOpen) {
            monarchy.openManually()
        }
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

    @MainThread
    override fun startSync() {
        assert(isOpen)
        if (!syncThread.isAlive) {
            syncThread.start()
        } else {
            syncThread.restart()
            Timber.w("Attempt to start an already started thread")
        }
    }

    @MainThread
    override fun stopSync() {
        assert(isOpen)
        syncThread.kill()
    }

    @MainThread
    override fun close() {
        assertMainThread()
        assert(isOpen)
        liveEntityObservers.forEach { it.dispose() }
        cryptoService.close()
        if (monarchy.isMonarchyThreadOpen) {
            monarchy.closeManually()
        }
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
        return signOutService.signOut(object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                Timber.w("SIGN_OUT: call webservice -> SUCCESS: clear cache")

                // Clear the cache
                cacheService.clearCache(object : MatrixCallback<Unit> {
                    override fun onSuccess(data: Unit) {
                        Timber.w("SIGN_OUT: clear cache -> SUCCESS: clear crypto cache")
                        cryptoService.clearCryptoCache(MatrixCallbackDelegate(callback))
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