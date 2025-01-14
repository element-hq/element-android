/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.helper

import android.os.Handler
import android.os.HandlerThread

private const val THREAD_NAME = "Vector-Timeline_Building_Thread"

object TimelineAsyncHelper {

    private var backgroundHandler: Handler? = null

    fun getBackgroundHandler(): Handler {
        return backgroundHandler ?: createBackgroundHandler().also { backgroundHandler = it }
    }

    private fun createBackgroundHandler(): Handler {
        val handlerThread = HandlerThread(THREAD_NAME)
        handlerThread.start()
        return Handler(handlerThread.looper)
    }
}
