/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.notifications

import android.content.Context
import org.matrix.android.sdk.api.Matrix
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

// TODO Multi-account
private const val ROOMS_NOTIFICATIONS_FILE_NAME = "im.vector.notifications.cache"
private const val KEY_ALIAS_SECRET_STORAGE = "notificationMgr"

class NotificationEventPersistence @Inject constructor(
        private val context: Context,
        private val matrix: Matrix,
) {

    fun loadEvents(factory: (List<NotifiableEvent>) -> NotificationEventQueue): NotificationEventQueue {
        try {
            val file = File(context.applicationContext.cacheDir, ROOMS_NOTIFICATIONS_FILE_NAME)
            if (file.exists()) {
                file.inputStream().use {
                    val events: ArrayList<NotifiableEvent>? = matrix.secureStorageService().loadSecureSecret(it, KEY_ALIAS_SECRET_STORAGE)
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

    fun persistEvents(queuedEvents: NotificationEventQueue) {
        if (queuedEvents.isEmpty()) {
            deleteCachedRoomNotifications(context)
            return
        }
        try {
            val file = File(context.applicationContext.cacheDir, ROOMS_NOTIFICATIONS_FILE_NAME)
            if (!file.exists()) file.createNewFile()
            FileOutputStream(file).use {
                matrix.secureStorageService().securelyStoreObject(queuedEvents.rawEvents(), KEY_ALIAS_SECRET_STORAGE, it)
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
