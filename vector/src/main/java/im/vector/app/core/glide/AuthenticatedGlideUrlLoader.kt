/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.glide

import android.content.Context
import com.bumptech.glide.integration.okhttp3.OkHttpStreamFetcher
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import im.vector.app.core.extensions.singletonEntryPoint
import okhttp3.Call
import okhttp3.OkHttpClient
import java.io.InputStream

class AuthenticatedGlideUrlLoaderFactory(private val context: Context) : ModelLoaderFactory<GlideUrl, InputStream> {

    private val defaultClient = OkHttpClient()

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl, InputStream> {
        return AuthenticatedGlideUrlLoader(context, defaultClient)
    }

    override fun teardown() = Unit
}

class AuthenticatedGlideUrlLoader(
        context: Context,
        private val defaultClient: OkHttpClient
) :
        ModelLoader<GlideUrl, InputStream> {

    private val activeSessionHolder = context.singletonEntryPoint().activeSessionHolder()
    private val client: OkHttpClient
        get() = activeSessionHolder.getSafeActiveSession()
                ?.getAuthenticatedOkHttpClient()
                ?: defaultClient

    private val callFactory = Call.Factory { request -> client.newCall(request) }

    override fun handles(model: GlideUrl): Boolean {
        if (!activeSessionHolder.hasActiveSession()) return false
        val contentUrlResolver = activeSessionHolder.getActiveSession().contentUrlResolver()
        val stringUrl = model.toStringUrl()
        return contentUrlResolver.requiresAuthentication(stringUrl)
    }

    override fun buildLoadData(model: GlideUrl, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream> {
        val fetcher = OkHttpStreamFetcher(callFactory, model)
        return ModelLoader.LoadData(model, fetcher)
    }
}
