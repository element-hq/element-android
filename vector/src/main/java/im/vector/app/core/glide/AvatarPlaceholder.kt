/*
 * Copyright (c) 2021 New Vector Ltd
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
import android.graphics.drawable.Drawable
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import im.vector.app.core.extensions.vectorComponent
import org.matrix.android.sdk.api.util.MatrixItem

data class AvatarPlaceholder(val matrixItem: MatrixItem)

class AvatarPlaceholderModelLoaderFactory(private val context: Context) : ModelLoaderFactory<AvatarPlaceholder, Drawable> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<AvatarPlaceholder, Drawable> {
        return AvatarPlaceholderModelLoader(context)
    }

    override fun teardown() {
        // Is there something to do here?
    }
}

class AvatarPlaceholderModelLoader(private val context: Context)
    : ModelLoader<AvatarPlaceholder, Drawable> {

    override fun buildLoadData(model: AvatarPlaceholder, width: Int, height: Int, options: Options): ModelLoader.LoadData<Drawable>? {
        return ModelLoader.LoadData(ObjectKey(model), AvatarPlaceholderDataFetcher(context, model))
    }

    override fun handles(model: AvatarPlaceholder): Boolean {
        return true
    }
}

class AvatarPlaceholderDataFetcher(context: Context, private val data: AvatarPlaceholder)
    : DataFetcher<Drawable> {

    private val avatarRenderer = context.vectorComponent().avatarRenderer()

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Drawable>) {
        val avatarPlaceholder = avatarRenderer.getPlaceholderDrawable(data.matrixItem)
        callback.onDataReady(avatarPlaceholder)
    }

    override fun cleanup() {
        // NOOP
    }

    override fun cancel() {
        // NOOP
    }

    override fun getDataClass(): Class<Drawable> {
        return Drawable::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.LOCAL
    }
}
