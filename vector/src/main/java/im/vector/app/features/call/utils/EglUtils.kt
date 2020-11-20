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
