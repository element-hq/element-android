/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.rageshake

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.os.Process
import java.lang.reflect.Method
import javax.inject.Inject

class ProcessInfo @Inject constructor() {
    fun getInfo() = buildString {
        append("===========================================\n")
        append("*              PROCESS INFO               *\n")
        append("===========================================\n")
        val processId = Process.myPid()
        append("ProcessId: $processId\n")
        append("ProcessName: ${getProcessName()}\n")
        append(getThreadInfo())
        append("===========================================\n")
    }

    @SuppressLint("PrivateApi")
    private fun getProcessName(): String? {
        return if (Build.VERSION.SDK_INT >= 28) {
            Application.getProcessName()
        } else {
            try {
                val activityThread = Class.forName("android.app.ActivityThread")
                val getProcessName: Method = activityThread.getDeclaredMethod("currentProcessName")
                getProcessName.invoke(null) as? String
            } catch (t: Throwable) {
                null
            }
        }
    }

    private fun getThreadInfo() = buildString {
        append("Thread activeCount: ${Thread.activeCount()}\n")
        Thread.getAllStackTraces().keys
                .sortedBy { it.name }
                .forEach { thread -> append(thread.getInfo()) }
    }
}

private fun Thread.getInfo() = buildString {
    append("Thread '$name':")
    append(" id: $id")
    append(" priority: $priority")
    append(" group name: ${threadGroup?.name ?: "null"}")
    append(" state: $state")
    append(" isAlive: $isAlive")
    append(" isDaemon: $isDaemon")
    append(" isInterrupted: $isInterrupted")
    append("\n")
}
