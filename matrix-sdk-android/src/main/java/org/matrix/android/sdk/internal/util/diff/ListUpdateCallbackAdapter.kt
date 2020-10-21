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

import androidx.recyclerview.widget.ListUpdateCallback

/**
 * Used to translate results of DiffUtil into 3 separate arrays.
 *
 */
internal class ListUpdateCallbackAdapter : ListUpdateCallback {

    var insertions: IntArray = IntArray(0)
    var deletions: IntArray = IntArray(0)
    var changes: IntArray = IntArray(0)

    override fun onInserted(position: Int, count: Int) {
        insertions = (position until position + count).toList().toIntArray()
    }

    override fun onRemoved(position: Int, count: Int) {
        deletions = (position until position + count).toList().toIntArray()
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        //Noop
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        changes = (position until position + count).toList().toIntArray()
    }
}
