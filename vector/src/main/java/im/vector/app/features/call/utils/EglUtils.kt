/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.call.utils

import org.webrtc.EglBase
import timber.log.Timber

/**
 * The root [EglBase] instance shared by the entire application for
 * the sake of reducing the utilization of system resources (such as EGL
 * contexts)
 * by performing a runtime check.
 */
object EglUtils {

    // TODO how do we release that?

    /**
     * Lazily creates and returns the one and only [EglBase] which will
     * serve as the root for all contexts that are needed.
     */
    @get:Synchronized var rootEglBase: EglBase? = null
        get() {
            if (field == null) {
                val configAttributes = EglBase.CONFIG_PLAIN
                try {
                    field = EglBase.createEgl14(configAttributes)
                            ?: EglBase.createEgl10(configAttributes) // Fall back to EglBase10.
                } catch (ex: Throwable) {
                    Timber.e(ex, "Failed to create EglBase")
                }
            }
            return field
        }
        private set

    val rootEglBaseContext: EglBase.Context?
        get() {
            val eglBase = rootEglBase
            return eglBase?.eglBaseContext
        }
}
