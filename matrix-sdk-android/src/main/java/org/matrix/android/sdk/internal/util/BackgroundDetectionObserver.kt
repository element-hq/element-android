/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArraySet

internal interface BackgroundDetectionObserver : DefaultLifecycleObserver {
    val isInBackground: Boolean

    fun register(listener: Listener)
    fun unregister(listener: Listener)

    interface Listener {
        fun onMoveToForeground()
        fun onMoveToBackground()
    }
}

internal class DefaultBackgroundDetectionObserver : BackgroundDetectionObserver {

    override var isInBackground: Boolean = true
        private set

    private val listeners = CopyOnWriteArraySet<BackgroundDetectionObserver.Listener>()

    override fun register(listener: BackgroundDetectionObserver.Listener) {
        listeners.add(listener)
    }

    override fun unregister(listener: BackgroundDetectionObserver.Listener) {
        listeners.remove(listener)
    }

    override fun onStart(owner: LifecycleOwner) {
        Timber.d("App returning to foreground…")
        isInBackground = false
        listeners.forEach { it.onMoveToForeground() }
    }

    override fun onStop(owner: LifecycleOwner) {
        Timber.d("App going to background…")
        isInBackground = true
        listeners.forEach { it.onMoveToBackground() }
    }
}
