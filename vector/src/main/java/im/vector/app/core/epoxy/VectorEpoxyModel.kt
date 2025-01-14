/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy

import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.airbnb.epoxy.VisibilityState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren

/**
 * EpoxyModelWithHolder which can listen to visibility state change.
 */
abstract class VectorEpoxyModel<H : VectorEpoxyHolder>(
        @LayoutRes private val layoutId: Int
) : EpoxyModelWithHolder<H>() {

    protected val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var onModelVisibilityStateChangedListener: OnVisibilityStateChangedListener? = null

    final override fun getDefaultLayout() = layoutId

    @CallSuper
    override fun bind(holder: H) {
        super.bind(holder)
    }

    @CallSuper
    override fun unbind(holder: H) {
        coroutineScope.coroutineContext.cancelChildren()
        super.unbind(holder)
    }

    override fun onVisibilityStateChanged(visibilityState: Int, view: H) {
        onModelVisibilityStateChangedListener?.onVisibilityStateChanged(visibilityState)
        super.onVisibilityStateChanged(visibilityState, view)
    }

    fun setOnVisibilityStateChanged(listener: OnVisibilityStateChangedListener): VectorEpoxyModel<H> {
        this.onModelVisibilityStateChangedListener = listener
        return this
    }

    interface OnVisibilityStateChangedListener {
        fun onVisibilityStateChanged(@VisibilityState.Visibility visibilityState: Int)
    }
}
