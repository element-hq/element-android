/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.features.notifications

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BitmapLoader @Inject constructor(val context: Context) {

    /**
     * Avatar Url -> Icon
     */
    private val cache = HashMap<String, Bitmap?>()

    // Black list of URLs (broken URL, etc.)
    private val blacklist = HashSet<String>()

    /**
     * Get icon of a room.
     * If already in cache, use it, else load it and call BitmapLoaderListener.onBitmapsLoaded() when ready
     */
    @WorkerThread
    fun getRoomBitmap(path: String?): Bitmap? {
        if (path == null) {
            return null
        }

        return cache.getOrPut(path) {
            loadRoomBitmap(path)
        }
    }

    @WorkerThread
    private fun loadRoomBitmap(path: String): Bitmap? {
        val bitmap = path.let {
            try {
                Glide.with(context)
                        .asBitmap()
                        .load(path)
                        .format(DecodeFormat.PREFER_ARGB_8888)
                        .submit()
                        .get()
            } catch (e: Exception) {
                Timber.e(e, "decodeFile failed")
                null
            }
        }

        if (bitmap == null) {
            // Add to the blacklist
            blacklist.add(path)
        }

        return bitmap
    }
}
