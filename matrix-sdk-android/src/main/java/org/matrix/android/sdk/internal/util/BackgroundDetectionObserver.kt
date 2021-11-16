/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.matrix.android.sdk.internal.di.MatrixScope
import timber.log.Timber
import javax.inject.Inject

/**
 * To be attached to ProcessLifecycleOwner lifecycle
 */
@MatrixScope
internal class BackgroundDetectionObserver @Inject constructor() : DefaultLifecycleObserver {

    var isInBackground: Boolean = true
        private set

    private val listeners = LinkedHashSet<Listener>()

    fun register(listener: Listener) {
        listeners.add(listener)
    }

    fun unregister(listener: Listener) {
        listeners.remove(listener)
    }

    override fun onStart(owner: LifecycleOwner) {
        Timber.v("App returning to foreground…")
        isInBackground = false
        listeners.forEach { it.onMoveToForeground() }
    }

    override fun onStop(owner: LifecycleOwner) {
        Timber.v("App going to background…")
        isInBackground = true
        listeners.forEach { it.onMoveToBackground() }
    }

    interface Listener {
        fun onMoveToForeground()
        fun onMoveToBackground()
    }
}
