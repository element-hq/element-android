/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.internal.util.diff

import androidx.recyclerview.widget.DiffUtil

/*
    Simple implementation of DiffUtilCallback.
    It relies on equals method for checking content.
 */
internal class SimpleDiffUtilCallback<T>(
        private val oldList: List<T>,
        private val newList: List<T>,
        private val areItemsTheSame: (T?, T?) -> Boolean
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList.getOrNull(oldItemPosition)
        val newItem = newList.getOrNull(newItemPosition)
        return areItemsTheSame(oldItem, newItem)
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList.getOrNull(oldItemPosition) == newList.getOrNull(newItemPosition)
    }
}
