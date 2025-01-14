/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.media

import com.github.piasy.biv.loader.ImageLoader
import java.io.File

interface DefaultImageLoaderCallback : ImageLoader.Callback {

    override fun onFinish() {
        // no-op
    }

    override fun onSuccess(image: File?) {
        // no-op
    }

    override fun onFail(error: Exception?) {
        // no-op
    }

    override fun onCacheHit(imageType: Int, image: File?) {
        // no-op
    }

    override fun onCacheMiss(imageType: Int, image: File?) {
        // no-op
    }

    override fun onProgress(progress: Int) {
        // no-op
    }

    override fun onStart() {
        // no-op
    }
}
