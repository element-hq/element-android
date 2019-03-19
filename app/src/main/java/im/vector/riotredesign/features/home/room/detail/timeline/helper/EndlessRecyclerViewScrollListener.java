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

package im.vector.riotredesign.features.home.room.detail.timeline.helper;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import im.vector.matrix.android.api.session.room.timeline.Timeline;

public abstract class EndlessRecyclerViewScrollListener extends RecyclerView.OnScrollListener {
    // Sets the starting page index
    private static final int startingPageIndex = 0;
    // The minimum amount of items to have below your current scroll position
    // before loading more.
    private int visibleThreshold = 50;
    // The total number of items in the dataset after the last load
    private int previousTotalItemCount = 0;
    // True if we are still waiting for the last set of data to load.
    private boolean loading = true;
    private LinearLayoutManager mLayoutManager;
    private Timeline.Direction mDirection;

    public EndlessRecyclerViewScrollListener(LinearLayoutManager layoutManager, Timeline.Direction direction) {
        this.mLayoutManager = layoutManager;
        this.mDirection = direction;
    }


    // This happens many times a second during a scroll, so be wary of the code you place here.
    // We are given a few useful parameters to help us work out if we need to load some more data,
    // but first we check if we are waiting for the previous load to finish.
    @Override
    public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
        int lastVisibleItemPosition = 0;
        int firstVisibleItemPosition = 0;
        int totalItemCount = mLayoutManager.getItemCount();

        lastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition();
        firstVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition();

        switch (mDirection) {
            case BACKWARDS:
                // If the total item count is zero and the previous isn't, assume the
                // list is invalidated and should be reset back to initial state
                if (totalItemCount < previousTotalItemCount) {
                    this.previousTotalItemCount = totalItemCount;
                    if (totalItemCount == 0) {
                        this.loading = true;
                    }
                }
                // If it’s still loading, we check to see if the dataset count has
                // changed, if so we conclude it has finished loading and update the current page
                // number and total item count.
                if (loading && (totalItemCount > previousTotalItemCount)) {
                    loading = false;
                    previousTotalItemCount = totalItemCount;
                }

                // If it isn’t currently loading, we check to see if we have breached
                // the visibleThreshold and need to reload more data.
                // If we do need to reload some more data, we execute onLoadMore to fetch the data.
                // threshold should reflect how many total columns there are too
                if (!loading && (lastVisibleItemPosition + visibleThreshold) > totalItemCount) {
                    onLoadMore();
                    loading = true;
                }
                break;
            case FORWARDS:
                // If the total item count is zero and the previous isn't, assume the
                // list is invalidated and should be reset back to initial state
                if (totalItemCount < previousTotalItemCount) {
                    this.previousTotalItemCount = totalItemCount;
                    if (totalItemCount == 0) {
                        this.loading = true;
                    }
                }
                // If it’s still loading, we check to see if the dataset count has
                // changed, if so we conclude it has finished loading and update the current page
                // number and total item count.
                if (loading && (totalItemCount > previousTotalItemCount)) {
                    loading = false;
                    previousTotalItemCount = totalItemCount;
                }

                // If it isn’t currently loading, we check to see if we have breached
                // the visibleThreshold and need to reload more data.
                // If we do need to reload some more data, we execute onLoadMore to fetch the data.
                // threshold should reflect how many total columns there are too
                if (!loading && firstVisibleItemPosition < visibleThreshold) {
                    onLoadMore();
                    loading = true;
                }
                break;
        }
    }

    // Defines the process for actually loading more data based on page
    public abstract void onLoadMore();

}