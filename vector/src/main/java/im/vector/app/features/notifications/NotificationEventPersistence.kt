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

package im.vector.app.features.notifications

import android.content.Context
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

// TODO Multi-account
private const val ROOMS_NOTIFICATIONS_FILE_NAME = "im.vector.notifications.cache"
private const val KEY_ALIAS_SECRET_STORAGE = "notificationMgr"

class NotificationEventPersistence @Inject constructor(private val context: Context) {

    fun loadEvents(currentSession: Session?, factory: (List<NotifiableEvent>) -> NotificationEventQueue): NotificationEventQueue {
        try {
            val file = File(context.applicationContext.cacheDir, ROOMS_NOTIFICATIONS_FILE_NAME)
            if (file.exists()) {
                file.inputStream().use {
                    val events: ArrayList<NotifiableEvent>? = currentSession?.loadSecureSecret(it, KEY_ALIAS_SECRET_STORAGE)
                    if (events != null) {
                        return factory(events)
                    }
                }
            }
        } catch (e: Throwable) {
            Timber.e(e, "## Failed to load cached notification info")
        }
        return factory(emptyList())
    }

    fun persistEvents(queuedEvents: NotificationEventQueue, currentSession: Session) {
        if (queuedEvents.isEmpty()) {
            deleteCachedRoomNotifications(context)
            return
        }
        try {
            val file = File(context.applicationContext.cacheDir, ROOMS_NOTIFICATIONS_FILE_NAME)
            if (!file.exists()) file.createNewFile()
            FileOutputStream(file).use {
                currentSession.securelyStoreObject(queuedEvents.rawEvents(), KEY_ALIAS_SECRET_STORAGE, it)
            }
        } catch (e: Throwable) {
            Timber.e(e, "## Failed to save cached notification info")
        }
    }

    private fun deleteCachedRoomNotifications(context: Context) {
        val file = File(context.applicationContext.cacheDir, ROOMS_NOTIFICATIONS_FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }
}
