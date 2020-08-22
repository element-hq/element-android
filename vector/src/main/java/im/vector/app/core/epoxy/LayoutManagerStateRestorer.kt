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

import android.os.Bundle
import android.os.Parcelable
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.core.platform.DefaultListUpdateCallback
import im.vector.app.core.platform.Restorable
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
        val parcelable = savedInstanceState?.getParcelable<Parcelable>(LAYOUT_MANAGER_STATE)
        layoutManagerState.set(parcelable)
    }

    override fun onInserted(position: Int, count: Int) {
        layoutManagerState.getAndSet(null)?.also {
            layoutManager?.onRestoreInstanceState(it)
        }
    }
}
