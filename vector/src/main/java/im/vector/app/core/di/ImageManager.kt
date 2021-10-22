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
import im.vector.app.ActiveSessionDataSource
import im.vector.app.core.glide.FactoryUrl
import org.matrix.android.sdk.api.session.Session
import java.io.InputStream
import javax.inject.Inject

/**
 * This class is used to configure the library we use for images
 */
class ImageManager @Inject constructor(
        private val context: Context,
        private val activeSessionDataSource: ActiveSessionDataSource
) {

    fun onSessionStarted(session: Session) {
        // Do this call first
        BigImageViewer.initialize(GlideImageLoader.with(context, session.getOkHttpClient()))

        val glide = Glide.get(context)

        // And this one. FIXME But are losing what BigImageViewer has done to add a Progress listener
        glide.registry.replace(GlideUrl::class.java, InputStream::class.java, FactoryUrl(activeSessionDataSource))
    }
}
