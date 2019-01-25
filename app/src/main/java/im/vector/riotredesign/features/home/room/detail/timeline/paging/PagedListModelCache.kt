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

package im.vector.riotredesign.features.home.room.detail.timeline.paging

import android.annotation.SuppressLint
import android.os.Handler
import androidx.paging.AsyncPagedListDiffer
import androidx.paging.PagedList
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.airbnb.epoxy.EpoxyModel
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A PagedList stream wrapper that caches models built for each item. It tracks changes in paged lists and caches
 * models for each item when they are invalidated to avoid rebuilding models for the whole list when PagedList is
 * updated.
 */
class PagedListModelCache<T>(
        private val modelBuilder: (itemIndex: Int, items: List<T>) -> List<EpoxyModel<*>>,
        private val rebuildCallback: () -> Unit,
        private val itemDiffCallback: DiffUtil.ItemCallback<T>,
        private val diffExecutor: Executor? = null,
        private val modelBuildingHandler: Handler
) {


    // Int is the index of the pagedList item
    // We have to be able to find the pagedlist position coming from an epoxy model to trigger
    // LoadAround with accuracy
    private val modelCache = linkedMapOf<EpoxyModel<*>, Int>()
    private var isCacheStale = AtomicBoolean(true)

    /**
     * Tracks the last accessed position so that we can report it back to the paged list when models are built.
     */
    private var lastPosition: Int? = null

    /**
     * Observer for the PagedList changes that invalidates the model cache when data is updated.
     */
    private val updateCallback = object : ListUpdateCallback {
        override fun onChanged(position: Int, count: Int, payload: Any?) {
            invalidate()
            rebuildCallback()
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            invalidate()
            rebuildCallback()
        }

        override fun onInserted(position: Int, count: Int) {
            invalidate()
            rebuildCallback()
        }

        override fun onRemoved(position: Int, count: Int) {
            invalidate()
            rebuildCallback()
        }
    }

    @SuppressLint("RestrictedApi")
    private val asyncDiffer = AsyncPagedListDiffer<T>(
            updateCallback,
            AsyncDifferConfig.Builder<T>(
                    itemDiffCallback
            ).also { builder ->
                if (diffExecutor != null) {
                    builder.setBackgroundThreadExecutor(diffExecutor)
                }
                // we have to reply on this private API, otherwise, paged list might be changed when models are being built,
                // potentially creating concurrent modification problems.
                builder.setMainThreadExecutor { runnable: Runnable ->
                    modelBuildingHandler.post(runnable)
                }
            }.build()
    )

    fun submitList(pagedList: PagedList<T>?) {
        asyncDiffer.submitList(pagedList)
    }

    fun getModels(): List<EpoxyModel<*>> {
        if (isCacheStale.compareAndSet(true, false)) {
            asyncDiffer.currentList?.forEachIndexed { position, _ ->
                buildModel(position)
            }
        }
        lastPosition?.let {
            triggerLoadAround(it)
        }
        return modelCache.keys.toList()
    }

    fun loadAround(model: EpoxyModel<*>) {
        modelCache[model]?.let { itemPosition ->
            triggerLoadAround(itemPosition)
            lastPosition = itemPosition
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun invalidate() {
        modelCache.clear()
        isCacheStale.set(true)
    }

    private fun cacheModelsAtPosition(itemPosition: Int, epoxyModels: Set<EpoxyModel<*>>) {
        epoxyModels.forEach {
            modelCache[it] = itemPosition
        }
    }

    private fun buildModel(pos: Int) {
        if (pos >= asyncDiffer.currentList?.size ?: 0) {
            return
        }
        modelBuilder(pos, asyncDiffer.currentList as List<T>).also {
            cacheModelsAtPosition(pos, it.toSet())
        }
    }

    private fun triggerLoadAround(position: Int) {
        asyncDiffer.currentList?.let {
            if (it.size > 0) {
                it.loadAround(Math.min(position, it.size - 1))
            }
        }
    }
}
