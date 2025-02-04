/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
