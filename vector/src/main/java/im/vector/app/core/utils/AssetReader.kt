/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.app.core.utils

import android.content.Context
import timber.log.Timber
import javax.inject.Inject

/**
 * Read asset files
 */
class AssetReader @Inject constructor(private val context: Context) {

    /* ==========================================================================================
     * CACHE
     * ========================================================================================== */
    private val cache = mutableMapOf<String, String?>()

    /**
     * Read an asset from resource and return a String or null in case of error.
     *
     * @param assetFilename Asset filename
     * @return the content of the asset file, or null in case of error
     */
    fun readAssetFile(assetFilename: String): String? {
        return cache.getOrPut(assetFilename, {
            return try {
                context.assets.open(assetFilename)
                        .use { asset ->
                            buildString {
                                var ch = asset.read()
                                while (ch != -1) {
                                    append(ch.toChar())
                                    ch = asset.read()
                                }
                            }
                        }
            } catch (e: Exception) {
                Timber.e(e, "## readAssetFile() failed")
                null
            }
        })
    }

    fun clearCache() {
        cache.clear()
    }
}
