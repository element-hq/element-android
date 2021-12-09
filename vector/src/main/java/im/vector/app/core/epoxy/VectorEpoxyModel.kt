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

package im.vector.app.core.epoxy

import androidx.annotation.CallSuper
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.airbnb.epoxy.VisibilityState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren

/**
 * EpoxyModelWithHolder which can listen to visibility state change
 */
abstract class VectorEpoxyModel<H : VectorEpoxyHolder> : EpoxyModelWithHolder<H>() {

    protected val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var onModelVisibilityStateChangedListener: OnVisibilityStateChangedListener? = null

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
