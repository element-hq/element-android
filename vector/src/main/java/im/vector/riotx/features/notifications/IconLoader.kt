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
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.WorkerThread
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.RequestOptions
import timber.log.Timber

/**
 * FIXME It works, but it does not refresh the notification, when it's already displayed
 */
class IconLoader(val context: Context,
                 val listener: IconLoaderListener) {

    /**
     * Avatar Url -> Icon
     */
    private val cache = HashMap<String, IconCompat>()

    // URLs to load
    private val toLoad = HashSet<String>()

    // Black list of URLs (broken URL, etc.)
    private val blacklist = HashSet<String>()

    private var uiHandler = Handler()

    private val handlerThread: HandlerThread = HandlerThread("IconLoader", Thread.MIN_PRIORITY)
    private var backgroundHandler: Handler

    init {
        handlerThread.start()
        backgroundHandler = Handler(handlerThread.looper)
    }

    /**
     * Get icon of a user.
     * If already in cache, use it, else load it and call IconLoaderListener.onIconsLoaded() when ready
     */
    fun getUserIcon(path: String?): IconCompat? {
        if (path == null) {
            return null
        }

        synchronized(cache) {
            if (cache[path] != null) {
                return cache[path]
            }

            // Add to the queue, if not blacklisted
            if (!blacklist.contains(path)) {
                if (toLoad.contains(path)) {
                    // Wait
                } else {
                    toLoad.add(path)

                    backgroundHandler.post {
                        loadUserIcon(path)
                    }
                }
            }
        }

        return null
    }

    @WorkerThread
    private fun loadUserIcon(path: String) {
        val iconCompat = path.let {
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

        synchronized(cache) {
            if (iconCompat == null) {
                // Add to the blacklist
                blacklist.add(path)
            } else {
                cache[path] = iconCompat
            }

            toLoad.remove(path)

            if (toLoad.isEmpty()) {
                uiHandler.post {
                    listener.onIconsLoaded()
                }
            }
        }
    }


    interface IconLoaderListener {
        fun onIconsLoaded()
    }
}