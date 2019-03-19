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

package im.vector.riotredesign.features.home.room.detail.timeline.helper

import androidx.recyclerview.widget.RecyclerView

class TimelineMediaSizeProvider {

    lateinit var recyclerView: RecyclerView
    private var cachedSize: Pair<Int, Int>? = null

    fun getMaxSize(): Pair<Int, Int> {
        return cachedSize ?: computeMaxSize().also { cachedSize = it }
    }

    private fun computeMaxSize(): Pair<Int, Int> {
        val width = recyclerView.width
        val height = recyclerView.height
        val maxImageWidth: Int
        val maxImageHeight: Int
        // landscape / portrait
        if (width < height) {
            maxImageWidth = Math.round(width * 0.7f)
            maxImageHeight = Math.round(height * 0.5f)
        } else {
            maxImageWidth = Math.round(width * 0.5f)
            maxImageHeight = Math.round(height * 0.7f)
        }
        return Pair(maxImageWidth, maxImageHeight)
    }


}