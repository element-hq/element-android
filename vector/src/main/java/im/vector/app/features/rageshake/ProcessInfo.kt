/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
