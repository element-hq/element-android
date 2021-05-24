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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import im.vector.app.R
import im.vector.app.core.extensions.vectorComponent
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.themes.ThemeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.extensions.tryOrNull
import xyz.belvi.blurhash.BlurHashDecoder

data class BlurHashData(
        val blurHash: String? = null
)

class BlurHashModelLoaderFactory(private val context: Context) : ModelLoaderFactory<BlurHashData, Drawable> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<BlurHashData, Drawable> {
        return BlurHashModelLoader(context)
    }

    override fun teardown() {
        // nop??
    }
}

class BlurHashModelLoader(val context: Context)
    : ModelLoader<BlurHashData, Drawable> {

    private val activeSessionHolder = context.vectorComponent().activeSessionHolder()

    override fun buildLoadData(model: BlurHashData, width: Int, height: Int, options: Options): ModelLoader.LoadData<Drawable> {
        return ModelLoader.LoadData(ObjectKey(model),
                BlurhashDataFetcher(context,
                        activeSessionHolder.getSafeActiveSession()?.coroutineScope ?: GlobalScope,
                        model.blurHash,
                        width,
                        height
                ))
    }

    override fun handles(model: BlurHashData): Boolean {
        return true
    }
}

class BlurhashDataFetcher(private val context: Context,
                          private val scope: CoroutineScope,
                          private val hash: String?,
                          private val width: Int,
                          private val height: Int)
    : DataFetcher<Drawable> {

    var job: Job? = null
    val defaultHash = "LEHV6nWB2yk8pyoJadR*.7kCMdnj"

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Drawable>) {
        job = scope.launch {
            withContext(Dispatchers.Default) {
                val result = runCatching {
                    (
                            tryOrNull { BlurHashDecoder.decode(hash ?: defaultHash, width, height) }
                                    ?: createImage(width, height, ThemeUtils.getColor(context, R.attr.riotx_reaction_background_off))
                            )
                            ?.let {
                                BitmapDrawable(
                                        context.resources,
                                        it
                                )
                            }
                }
                withContext(Dispatchers.Main) {
                    result.fold(
                            { callback.onDataReady(it) },
                            { callback.onLoadFailed(Exception(it)) }
                    )
                }
            }
        }
    }

    override fun getDataSource(): DataSource {
        // ?
        return DataSource.LOCAL
    }

    override fun cleanup() {
        // nop?
    }

    override fun cancel() {
        job?.cancel()
    }

    override fun getDataClass(): Class<Drawable> {
        return Drawable::class.java
    }

    fun createImage(width: Int, height: Int, color: Int): Bitmap? {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.setColor(color)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
}
