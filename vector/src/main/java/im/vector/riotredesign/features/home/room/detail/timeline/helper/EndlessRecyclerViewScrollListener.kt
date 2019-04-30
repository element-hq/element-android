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

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import im.vector.matrix.android.api.session.room.timeline.Timeline

class EndlessRecyclerViewScrollListener(private val layoutManager: LinearLayoutManager,
                                        private val visibleThreshold: Int,
                                        private val onLoadMore: (Timeline.Direction) -> Unit
) : RecyclerView.OnScrollListener() {

    // The total number of items in the dataset after the last load
    private var previousTotalItemCount = 0
    // True if we are still waiting for the last set of data to load.
    private var loadingBackwards = true
    private var loadingForwards = true

    // This happens many times a second during a scroll, so be wary of the code you place here.
    // We are given a few useful parameters to help us work out if we need to load some more data,
    // but first we check if we are waiting for the previous load to finish.

    override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
        val totalItemCount = layoutManager.itemCount

        // We check to see if the dataset count has
        // changed, if so we conclude it has finished loading
        if (totalItemCount != previousTotalItemCount) {
            previousTotalItemCount = totalItemCount
            loadingBackwards = false
            loadingForwards = false
        }
        // If it isn’t currently loading, we check to see if we have reached
        // the visibleThreshold and need to reload more data.
        if (!loadingBackwards && lastVisibleItemPosition + visibleThreshold > totalItemCount) {
            loadingBackwards = true
            onLoadMore(Timeline.Direction.BACKWARDS)
        }
        if (!loadingForwards && firstVisibleItemPosition < visibleThreshold) {
            loadingForwards = true
            onLoadMore(Timeline.Direction.FORWARDS)
        }
    }


}