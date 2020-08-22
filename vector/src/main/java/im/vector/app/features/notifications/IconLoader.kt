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

package im.vector.app.features.notifications

import android.content.Context
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.RequestOptions
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconLoader @Inject constructor(private val context: Context) {

    /**
     * Avatar Url -> IconCompat
     */
    private val cache = HashMap<String, IconCompat?>()

    /**
     * Get icon of a user.
     * If already in cache, use it, else load it and call IconLoaderListener.onIconsLoaded() when ready
     * Before Android P, this does nothing because the icon won't be used
     */
    @WorkerThread
    fun getUserIcon(path: String?): IconCompat? {
        if (path == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null
        }

        return cache.getOrPut(path) {
            loadUserIcon(path)
        }
    }

    @WorkerThread
    private fun loadUserIcon(path: String): IconCompat? {
        return path.let {
            try {
                Glide.with(context)
                        .asBitmap()
                        .load(path)
                        .apply(RequestOptions.circleCropTransform()
                                .format(DecodeFormat.PREFER_ARGB_8888))
                        .submit()
                        .get()
            } catch (e: Exception) {
                Timber.e(e, "decodeFile failed")
                null
            }?.let { bitmap ->
                IconCompat.createWithBitmap(bitmap)
            }
        }
    }
}
