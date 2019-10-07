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

package im.vector.riotx.core.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import im.vector.matrix.android.internal.crypto.attachments.MXEncryptedAttachments
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.features.media.ImageContentRenderer
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream


class VectorGlideModelLoaderFactory(private val activeSessionHolder: ActiveSessionHolder)
    : ModelLoaderFactory<ImageContentRenderer.Data, InputStream> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<ImageContentRenderer.Data, InputStream> {
        return VectorGlideModelLoader(activeSessionHolder)
    }

    override fun teardown() {
        // Is there something to do here?
    }

}

class VectorGlideModelLoader(private val activeSessionHolder: ActiveSessionHolder)
    : ModelLoader<ImageContentRenderer.Data, InputStream> {
    override fun handles(model: ImageContentRenderer.Data): Boolean {
        // Always handle
        return true
    }

    override fun buildLoadData(model: ImageContentRenderer.Data, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        return ModelLoader.LoadData(ObjectKey(model), VectorGlideDataFetcher(activeSessionHolder, model, width, height))
    }
}

class VectorGlideDataFetcher(private val activeSessionHolder: ActiveSessionHolder,
                             private val data: ImageContentRenderer.Data,
                             private val width: Int,
                             private val height: Int)
    : DataFetcher<InputStream> {

    val client = OkHttpClient()

    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }

    private var stream: InputStream? = null

    override fun cleanup() {
        cancel()
    }

    override fun getDataSource(): DataSource {
        // ?
        return DataSource.REMOTE
    }

    override fun cancel() {
        if (stream != null) {
            try {
                stream?.close() // interrupts decode if any
                stream = null
            } catch (ignore: IOException) {
                Timber.e(ignore)
            }
        }
    }

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        Timber.v("Load data: $data")
        if (data.isLocalFile()) {
            val initialFile = File(data.url)
            callback.onDataReady(FileInputStream(initialFile))
            return
        }
        val contentUrlResolver = activeSessionHolder.getActiveSession().contentUrlResolver()
        val url = contentUrlResolver.resolveFullSize(data.url)
                ?: return

        val request = Request.Builder()
                .url(url)
                .build()

        val response = client.newCall(request).execute()
        val inputStream = response.body?.byteStream()
        Timber.v("Response size ${response.body?.contentLength()} - Stream available: ${inputStream?.available()}")
        if (!response.isSuccessful) {
            callback.onLoadFailed(IOException("Unexpected code $response"))
            return
        }
        stream = if (data.elementToDecrypt != null && data.elementToDecrypt.k.isNotBlank()) {
            MXEncryptedAttachments.decryptAttachment(inputStream, data.elementToDecrypt)
        } else {
            inputStream
        }
        callback.onDataReady(stream)
    }
}