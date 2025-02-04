/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import android.content.Context
import timber.log.Timber
import javax.inject.Inject

/**
 * Read asset files.
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
