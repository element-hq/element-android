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
import io.realm.RealmChangeListener
import io.realm.RealmObject
import io.realm.RealmResults

internal abstract class DiffRealmChangeListener<T : RealmObject>(
        private var previousResults: List<T> = emptyList(),
) : RealmChangeListener<RealmResults<T>> {

    override fun onChange(results: RealmResults<T>) {
        if (!results.isLoaded || !results.isValid) {
            return
        }
        val snapshotResults = results.createSnapshot()
        val diffCallback = SimpleDiffUtilCallback<T>(previousResults, snapshotResults) { old, new ->
            areSameItems(old, new)
        }
        previousResults = snapshotResults
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        val listUpdateCallbackAdapter = ListUpdateCallbackAdapter()
        diffResult.dispatchUpdatesTo(listUpdateCallbackAdapter)
        handleResults(listUpdateCallbackAdapter)
    }

    abstract fun areSameItems(old: T?, new: T?): Boolean

    abstract fun handleResults(listUpdateCallbackAdapter: ListUpdateCallbackAdapter)
}
