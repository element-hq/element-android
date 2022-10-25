/*
 * Copyright (c) 2022 New Vector Ltd
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

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * This observer detects when item was added or moved to the first position of the adapter, while recyclerView is scrolled to the top. This is necessary
 * to force recycler to scroll to the top to make such item visible, because by default it will keep items on screen, while adding new item to the top,
 * outside of the viewport
 * @param layoutManager - [LinearLayoutManager] of the recycler view, which displays items
 * @property onItemUpdated - callback to be called, when observer detects event
 */
class FirstItemUpdatedObserver(
        layoutManager: LinearLayoutManager,
        private val onItemUpdated: () -> Unit
) : RecyclerView.AdapterDataObserver() {

    val layoutManager: LinearLayoutManager? by weak(layoutManager)

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        if ((toPosition == 0 || fromPosition == 0) && layoutManager?.findFirstCompletelyVisibleItemPosition() == 0) {
            onItemUpdated.invoke()
        }
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        if (positionStart == 0 && layoutManager?.findFirstCompletelyVisibleItemPosition() == 0) {
            onItemUpdated.invoke()
        }
    }
}
