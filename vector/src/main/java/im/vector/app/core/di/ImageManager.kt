/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.core.di

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import im.vector.app.core.glide.AuthenticatedGlideUrlLoaderFactory
import org.matrix.android.sdk.api.session.Session
import java.io.InputStream
import javax.inject.Inject

/**
 * This class is used to configure the library we use for images.
 */
class ImageManager @Inject constructor(
        private val context: Context,
) {

    fun onSessionStarted(session: Session) {
        // Do this call first
        val glideImageLoader = GlideImageLoader.with(context, session.getOkHttpClient())
        BigImageViewer.initialize(glideImageLoader)

        val glide = Glide.get(context)

        // And this one. It'll be tried first, otherwise it'll use the one initialised by GlideImageLoader.
        glide.registry.prepend(GlideUrl::class.java, InputStream::class.java, AuthenticatedGlideUrlLoaderFactory(context))
    }
}
