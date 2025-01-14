/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyVisibilityTracker

/**
 * Apply a Vertical LinearLayout Manager to the recyclerView and set the adapter from the epoxy controller.
 */
fun RecyclerView.configureWith(
        epoxyController: EpoxyController,
        itemAnimator: RecyclerView.ItemAnimator? = null,
        viewPool: RecyclerView.RecycledViewPool? = null,
        @DrawableRes
        dividerDrawable: Int? = null,
        hasFixedSize: Boolean = true,
        disableItemAnimation: Boolean = false
) {
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false).apply {
        recycleChildrenOnDetach = viewPool != null
    }
    setRecycledViewPool(viewPool)
    if (disableItemAnimation) {
        this.itemAnimator = null
    } else {
        itemAnimator?.let { this.itemAnimator = it }
    }
    dividerDrawable?.let { divider ->
        addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                    ContextCompat.getDrawable(context, divider)?.let {
                        setDrawable(it)
                    }
                }
        )
    }
    setHasFixedSize(hasFixedSize)
    adapter = epoxyController.adapter
}

/**
 * To call from Fragment.onDestroyView().
 */
fun RecyclerView.cleanup() {
    adapter = null
}

fun RecyclerView.trackItemsVisibilityChange() = EpoxyVisibilityTracker().attach(this)
