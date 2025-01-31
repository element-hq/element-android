/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.glide

import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import im.vector.app.ActiveSessionDataSource
import okhttp3.OkHttpClient
import java.io.InputStream

class FactoryUrl(private val activeSessionDataSource: ActiveSessionDataSource) : ModelLoaderFactory<GlideUrl, InputStream> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl, InputStream> {
        val client = activeSessionDataSource.currentValue?.orNull()?.getOkHttpClient() ?: OkHttpClient()
        return OkHttpUrlLoader(client)
    }

    override fun teardown() {
        // Do nothing, this instance doesn't own the client.
    }
}
