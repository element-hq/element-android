/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.detail.timeline.helper

import android.content.res.Resources
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.scopes.ActivityScoped
import im.vector.app.R
import im.vector.app.features.settings.VectorPreferences
import javax.inject.Inject
import kotlin.math.roundToInt

@ActivityScoped
class TimelineMediaSizeProvider @Inject constructor(private val resources: Resources,
                                                    private val vectorPreferences: VectorPreferences) {

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
            val bubbleMaxImageWidth = maxImageWidth.coerceAtMost(resources.getDimensionPixelSize(R.dimen.chat_bubble_fixed_size))
            Pair(bubbleMaxImageWidth, maxImageHeight)
        } else {
            Pair(maxImageWidth, maxImageHeight)
        }
    }
}
