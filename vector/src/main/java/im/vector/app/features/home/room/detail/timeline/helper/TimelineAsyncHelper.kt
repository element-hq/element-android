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

package im.vector.app.features.home.room.detail.timeline.helper

import android.os.Handler
import android.os.HandlerThread

private const val THREAD_NAME = "Timeline_Building_Thread"

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
