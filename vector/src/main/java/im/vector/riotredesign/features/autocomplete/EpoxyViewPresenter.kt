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

package im.vector.riotredesign.features.autocomplete

import android.content.Context
import android.database.DataSetObserver
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyRecyclerView
import com.otaliastudios.autocomplete.AutocompleteCallback
import com.otaliastudios.autocomplete.AutocompletePresenter

abstract class EpoxyViewPresenter<T>(context: Context) : AutocompletePresenter<T>(context) {

    private var recyclerView: EpoxyRecyclerView? = null
    private var clicks: AutocompletePresenter.ClickProvider<T>? = null
    private var observer: Observer? = null

    override fun registerClickProvider(provider: AutocompletePresenter.ClickProvider<T>) {
        this.clicks = provider
    }

    override fun registerDataSetObserver(observer: DataSetObserver) {
        this.observer = Observer(observer)
    }

    override fun getView(): ViewGroup? {
        recyclerView = EpoxyRecyclerView(context).apply {
            setController(providesController())
            observer?.let {
                adapter?.registerAdapterDataObserver(it)
            }
            itemAnimator = null
        }
        return recyclerView
    }

    override fun onViewShown() {}


    override fun onViewHidden() {
        recyclerView = null
        observer = null
    }

    abstract fun providesController(): EpoxyController
    /**
     * Dispatch click event to [AutocompleteCallback].
     * Should be called when items are clicked.
     *
     * @param item the clicked item.
     */
    protected fun dispatchClick(item: T) {
        clicks?.click(item)
    }

    protected fun dispatchLayoutChange() {
        observer?.onChanged()
    }


    private class Observer internal constructor(private val root: DataSetObserver) : RecyclerView.AdapterDataObserver() {

        override fun onChanged() {
            root.onChanged()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            root.onChanged()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            root.onChanged()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            root.onChanged()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            root.onChanged()
        }
    }
}