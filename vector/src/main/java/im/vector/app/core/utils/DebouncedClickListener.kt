/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.app.core.utils

import android.os.SystemClock
import android.view.View
import timber.log.Timber
import java.util.WeakHashMap

/**
 * Simple Debounced OnClickListener
 * Safe to use in different views
 */
class DebouncedClickListener(
        val original: View.OnClickListener,
        private val minimumInterval: Long = 400
) : View.OnClickListener {
    private val lastClickMap = WeakHashMap<View, Long>()

    override fun onClick(v: View) {
        val previousClickTimestamp = lastClickMap[v] ?: 0
        val currentTimestamp = SystemClock.elapsedRealtime()
        lastClickMap[v] = currentTimestamp

        if (currentTimestamp > previousClickTimestamp + minimumInterval) {
            original.onClick(v)
        } else {
            Timber.v("Debounced click!")
        }
    }
}
