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

package im.vector.app.core.glide

import android.content.Context
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import im.vector.app.core.extensions.vectorComponent
import im.vector.app.core.files.LocalFilesHelper
import im.vector.app.features.media.ImageContentRenderer
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.IOException
import java.io.InputStream

class VectorGlideModelLoaderFactory(private val context: Context) : ModelLoaderFactory<ImageContentRenderer.Data, InputStream> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<ImageContentRenderer.Data, InputStream> {
        return VectorGlideModelLoader(context)
    }

    override fun teardown() {
        // Is there something to do here?
    }
}

class VectorGlideModelLoader(private val context: Context)
    : ModelLoader<ImageContentRenderer.Data, InputStream> {
    override fun handles(model: ImageContentRenderer.Data): Boolean {
        // Always handle
        return true
    }

    override fun buildLoadData(model: ImageContentRenderer.Data, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        return ModelLoader.LoadData(ObjectKey(model), VectorGlideDataFetcher(context, model, width, height))
    }
}

class VectorGlideDataFetcher(context: Context,
                             private val data: ImageContentRenderer.Data,
                             private val width: Int,
                             private val height: Int)
    : DataFetcher<InputStream> {

    private val localFilesHelper = LocalFilesHelper(context)
    private val activeSessionHolder = context.vectorComponent().activeSessionHolder()

    private val client = activeSessionHolder.getSafeActiveSession()?.getOkHttpClient() ?: OkHttpClient()

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
                // This is often called on main thread, and this could be a network Stream..
                // on close will throw android.os.NetworkOnMainThreadException, so we catch throwable
                stream?.close() // interrupts decode if any
                stream = null
            } catch (ignore: Throwable) {
                Timber.e("Failed to close stream ${ignore.localizedMessage}")
            } finally {
                stream = null
            }
        }
    }

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        Timber.v("Load data: $data")
        if (localFilesHelper.isLocalFile(data.url)) {
            localFilesHelper.openInputStream(data.url)?.use {
                callback.onDataReady(it)
            }
            return
        }
//        val contentUrlResolver = activeSessionHolder.getActiveSession().contentUrlResolver()

        val fileService = activeSessionHolder.getSafeActiveSession()?.fileService() ?: return Unit.also {
            callback.onLoadFailed(IllegalArgumentException("No File service"))
        }
        // Use the file vector service, will avoid flickering and redownload after upload
        activeSessionHolder.getSafeActiveSession()?.coroutineScope?.launch {
            val result = runCatching {
                fileService.downloadFile(
                        fileName = data.filename,
                        mimeType = data.mimeType,
                        url = data.url,
                        elementToDecrypt = data.elementToDecrypt)
            }
            withContext(Dispatchers.Main) {
                result.fold(
                        { callback.onDataReady(it.inputStream()) },
                        { callback.onLoadFailed(it as? Exception ?: IOException(it.localizedMessage)) }
                )
            }
        }
//        val url = contentUrlResolver.resolveFullSize(data.url)
//                ?: return
//
//        val request = Request.Builder()
//                .url(url)
//                .build()
//
//        val response = client.newCall(request).execute()
//        val inputStream = response.body?.byteStream()
//        Timber.v("Response size ${response.body?.contentLength()} - Stream available: ${inputStream?.available()}")
//        if (!response.isSuccessful) {
//            callback.onLoadFailed(IOException("Unexpected code $response"))
//            return
//        }
//        stream = if (data.elementToDecrypt != null && data.elementToDecrypt.k.isNotBlank()) {
//            Matrix.decryptStream(inputStream, data.elementToDecrypt)
//        } else {
//            inputStream
//        }
//        callback.onDataReady(stream)
    }
}
