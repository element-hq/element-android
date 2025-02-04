/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
