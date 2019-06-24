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

package im.vector.riotredesign.core.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import im.vector.matrix.android.internal.crypto.attachments.ElementToDecrypt
import im.vector.matrix.android.internal.crypto.attachments.MXEncryptedAttachments
import java.io.InputStream
import com.bumptech.glide.load.engine.Resource as Resource1

class VectorGlideModelLoaderFactory : ModelLoaderFactory<InputStream, InputStream> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<InputStream, InputStream> {
        return VectorGlideModelLoader()
    }

    override fun teardown() {
        // Is there something to do here?
    }

}

class VectorGlideModelLoader : ModelLoader<InputStream, InputStream> {
    override fun handles(model: InputStream): Boolean {
        // Always handle
        return true
    }

    override fun buildLoadData(model: InputStream, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        return ModelLoader.LoadData(ObjectKey(model), VectorGlideDataFetcher(model, options.get(ELEMENT_TO_DECRYPT)))
    }
}

class VectorGlideDataFetcher(private val inputStream: InputStream,
                             private val elementToDecrypt: ElementToDecrypt?) : DataFetcher<InputStream> {
    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }

    override fun cleanup() {
        // ?
    }

    override fun getDataSource(): DataSource {
        // ?
        return DataSource.REMOTE
    }

    override fun cancel() {
        // ?
    }

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        if (elementToDecrypt?.k?.isNotBlank() == true) {
            // Encrypted stream
            callback.onDataReady(MXEncryptedAttachments.decryptAttachment(inputStream, elementToDecrypt))
        } else {
            // Not encrypted stream
            callback.onDataReady(inputStream)
        }
    }
}