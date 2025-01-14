/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.helper

import android.content.res.Resources
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.scopes.ActivityScoped
import im.vector.app.features.settings.VectorPreferences
import javax.inject.Inject
import kotlin.math.roundToInt

@ActivityScoped
class TimelineMediaSizeProvider @Inject constructor(
        private val resources: Resources,
        private val vectorPreferences: VectorPreferences
) {

    var recyclerView: RecyclerView? = null
    private var cachedSize: Pair<Int, Int>? = null

    fun getMaxSize(): Pair<Int, Int> {
        return cachedSize ?: computeMaxSize().also { cachedSize = it }
    }

    private fun computeMaxSize(): Pair<Int, Int> {
        val width = recyclerView?.width ?: 0
        val height = recyclerView?.height ?: 0
        val maxImageWidth: Int
        val maxImageHeight: Int
        // landscape / portrait
        if (width < height) {
            maxImageWidth = (width * 0.7f).roundToInt()
            maxImageHeight = (height * 0.5f).roundToInt()
        } else {
            maxImageWidth = (width * 0.7f).roundToInt()
            maxImageHeight = (height * 0.7f).roundToInt()
        }
        return if (vectorPreferences.useMessageBubblesLayout()) {
            val bubbleMaxImageWidth = maxImageWidth.coerceAtMost(resources.getDimensionPixelSize(im.vector.lib.ui.styles.R.dimen.chat_bubble_fixed_size))
            Pair(bubbleMaxImageWidth, maxImageHeight)
        } else {
            Pair(maxImageWidth, maxImageHeight)
        }
    }
}
