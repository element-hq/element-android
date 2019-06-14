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

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.cache.CacheService
import im.vector.matrix.android.api.session.content.ContentUploadStateTracker
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.room.RoomDirectoryService
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.api.session.signout.SignOutService
import im.vector.matrix.android.api.session.sync.FilterService
import im.vector.matrix.android.api.session.user.UserService
import im.vector.matrix.android.internal.crypto.CryptoManager
import im.vector.matrix.android.internal.database.LiveEntityObserver
import im.vector.matrix.android.internal.di.MatrixScope
import im.vector.matrix.android.internal.session.sync.job.SyncThread
import javax.inject.Inject

@MatrixScope
internal class SessionFactory @Inject constructor(
        private val monarchy: Monarchy,
        private val liveEntityUpdaters: List<LiveEntityObserver>,
        private val sessionListeners: SessionListeners,
        private val roomService: RoomService,
        private val roomDirectoryService: RoomDirectoryService,
        private val groupService: GroupService,
        private val userService: UserService,
        private val filterService: FilterService,
        private val cacheService: CacheService,
        private val signOutService: SignOutService,
        private val cryptoService: CryptoManager,
        private val syncThread: SyncThread,
        private val contentUrlResolver: ContentUrlResolver,
        private val contentUploadProgressTracker: ContentUploadStateTracker) {


    fun create(sessionParams: SessionParams): Session {
        return DefaultSession(
                sessionParams,
                monarchy,
                liveEntityUpdaters,
                sessionListeners,
                roomService,
                roomDirectoryService,
                groupService,
                userService,
                filterService,
                cacheService,
                signOutService,
                cryptoService,
                syncThread,
                contentUrlResolver,
                contentUploadProgressTracker
        )
    }

}