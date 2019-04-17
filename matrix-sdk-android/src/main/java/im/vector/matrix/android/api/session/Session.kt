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

package im.vector.matrix.android.api.session

import androidx.annotation.MainThread
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.content.ContentUploadStateTracker
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.api.session.signout.SignOutService
import im.vector.matrix.android.api.session.sync.FilterService
import im.vector.matrix.android.api.session.user.UserService

/**
 * This interface defines interactions with a session.
 * An instance of a session will be provided by the SDK.
 */
interface Session :
        RoomService,
        GroupService,
        UserService,
        CryptoService,
        SignOutService,
        FilterService {

    /**
     * The params associated to the session
     */
    val sessionParams: SessionParams

    /**
     * This method allow to open a session. It does start some service on the background.
     */
    @MainThread
    fun open()

    /**
     * This method start the sync thread.
     */
    @MainThread
    fun startSync()

    /**
     * This method stop the sync thread.
     */
    @MainThread
    fun stopSync()

    /**
     * This method allow to close a session. It does stop some services.
     */
    @MainThread
    fun close()

    /**
     * Returns the ContentUrlResolver associated to the session.
     */
    fun contentUrlResolver(): ContentUrlResolver

    /**
     * Returns the ContentUploadProgressTracker associated with the session
     */
    fun contentUploadProgressTracker(): ContentUploadStateTracker

    /**
     * Add a listener to the session.
     * @param listener the listener to add.
     */
    fun addListener(listener: Listener)

    /**
     * Remove a listener from the session.
     * @param listener the listener to remove.
     */
    fun removeListener(listener: Listener)

    /**
     * A global session listener to get notified for some events.
     */
    // Not used at the moment
    interface Listener

}