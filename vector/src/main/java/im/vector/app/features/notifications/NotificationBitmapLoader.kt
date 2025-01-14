/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.notifications

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.signature.ObjectKey
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationBitmapLoader @Inject constructor(private val context: Context) {

    /**
     * Get icon of a room.
     */
    @WorkerThread
    fun getRoomBitmap(path: String?): Bitmap? {
        if (path == null) {
            return null
        }
        return loadRoomBitmap(path)
    }

    @WorkerThread
    private fun loadRoomBitmap(path: String): Bitmap? {
        return try {
            Glide.with(context)
                    .asBitmap()
                    .load(path)
                    .format(DecodeFormat.PREFER_ARGB_8888)
                    .signature(ObjectKey("room-icon-notification"))
                    .submit()
                    .get()
        } catch (e: Exception) {
            Timber.e(e, "decodeFile failed")
            null
        }
    }

    /**
     * Get icon of a user.
     * Before Android P, this does nothing because the icon won't be used
     */
    @WorkerThread
    fun getUserIcon(path: String?): IconCompat? {
        if (path == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null
        }

        return loadUserIcon(path)
    }

    @WorkerThread
    private fun loadUserIcon(path: String): IconCompat? {
        return try {
            val bitmap = Glide.with(context)
                    .asBitmap()
                    .load(path)
                    .transform(CircleCrop())
                    .format(DecodeFormat.PREFER_ARGB_8888)
                    .signature(ObjectKey("user-icon-notification"))
                    .submit()
                    .get()
            IconCompat.createWithBitmap(bitmap)
        } catch (e: Exception) {
            Timber.e(e, "decodeFile failed")
            null
        }
    }
}
