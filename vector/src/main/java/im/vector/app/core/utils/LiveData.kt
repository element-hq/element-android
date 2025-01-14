/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

fun <T1, T2, R> combineLatest(source1: LiveData<T1>, source2: LiveData<T2>, mapper: (T1, T2) -> R): LiveData<R> {
    val combined = MediatorLiveData<R>()
    var source1Result: T1? = null
    var source2Result: T2? = null

    fun notify() {
        if (source1Result != null && source2Result != null) {
            combined.value = mapper(source1Result!!, source2Result!!)
        }
    }

    combined.addSource(source1) {
        source1Result = it
        notify()
    }
    combined.addSource(source2) {
        source2Result = it
        notify()
    }
    return combined
}
