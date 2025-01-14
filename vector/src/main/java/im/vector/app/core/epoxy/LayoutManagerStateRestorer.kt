/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy

import android.os.Bundle
import android.os.Parcelable
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.core.platform.DefaultListUpdateCallback
import im.vector.app.core.platform.Restorable
import im.vector.lib.core.utils.compat.getParcelableCompat
import java.util.concurrent.atomic.AtomicReference

private const val LAYOUT_MANAGER_STATE = "LAYOUT_MANAGER_STATE"

class LayoutManagerStateRestorer(layoutManager: RecyclerView.LayoutManager) : Restorable, DefaultListUpdateCallback {

    private var layoutManager: RecyclerView.LayoutManager? = null
    private var layoutManagerState = AtomicReference<Parcelable?>()

    init {
        this.layoutManager = layoutManager
    }

    fun clear() {
        layoutManager = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val layoutManagerState = layoutManager?.onSaveInstanceState()
        outState.putParcelable(LAYOUT_MANAGER_STATE, layoutManagerState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        val parcelable = savedInstanceState?.getParcelableCompat<Parcelable>(LAYOUT_MANAGER_STATE)
        layoutManagerState.set(parcelable)
    }

    override fun onInserted(position: Int, count: Int) {
        layoutManagerState.getAndSet(null)?.also {
            layoutManager?.onRestoreInstanceState(it)
        }
    }
}
