/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings.troubleshoot

import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import kotlin.properties.Delegates

class NotificationTroubleshootTestManager(val fragment: Fragment) {
    private val testList = ArrayList<TroubleshootTest>()

    val testListSize: Int
        get() = testList.size

    var isCancelled = false
        private set

    var currentTestIndex by Delegates.observable(0) { _, _, _ ->
        statusListener?.invoke(this)
    }
        private set

    val adapter = NotificationTroubleshootRecyclerViewAdapter(testList)

    var statusListener: ((NotificationTroubleshootTestManager) -> Unit)? = null

    var diagStatus: TroubleshootTest.TestStatus by Delegates.observable(TroubleshootTest.TestStatus.NOT_STARTED) { _, _, _ ->
        statusListener?.invoke(this)
    }
        private set

    fun addTest(test: TroubleshootTest) {
        testList.add(test)
        test.manager = this
    }

    fun runDiagnostic(testParameters: TroubleshootTest.TestParameters) {
        if (isCancelled) return
        currentTestIndex = 0
        val handler = Handler(Looper.getMainLooper())
        diagStatus = if (testList.size > 0) TroubleshootTest.TestStatus.RUNNING else TroubleshootTest.TestStatus.SUCCESS
        var isAllGood = true
        for ((index, test) in testList.withIndex()) {
            test.statusListener = {
                if (!isCancelled) {
                    adapter.notifyItemChanged(index)
                    if (it.isFinished()) {
                        isAllGood = isAllGood && (it.status == TroubleshootTest.TestStatus.SUCCESS)
                        currentTestIndex++
                        if (currentTestIndex < testList.size) {
                            val troubleshootTest = testList[currentTestIndex]
                            troubleshootTest.status = TroubleshootTest.TestStatus.RUNNING
                            // Cosmetic: Start with a small delay for UI/UX reason (better animation effect) for non async tests
                            handler.postDelayed({
                                if (fragment.isAdded) {
                                    troubleshootTest.perform(testParameters)
                                }
                            }, 600)
                        } else {
                            // we are done, test global status?
                            diagStatus = if (isAllGood) TroubleshootTest.TestStatus.SUCCESS else TroubleshootTest.TestStatus.FAILED
                        }
                    }
                }
            }
        }
        if (fragment.isAdded) {
            testList.firstOrNull()?.perform(testParameters)
        }
    }

    fun retry(testParameters: TroubleshootTest.TestParameters) {
        testList.forEach {
            it.cancel()
            it.description = null
            it.quickFix = null
            it.status = TroubleshootTest.TestStatus.NOT_STARTED
        }
        runDiagnostic(testParameters)
    }

    fun hasQuickFix(): Boolean {
        return testList.any { test ->
            test.status == TroubleshootTest.TestStatus.FAILED && test.quickFix != null
        }
    }

    fun cancel() {
        isCancelled = true
        testList.forEach { it.cancel() }
    }

    fun onDiagnosticPushReceived() {
        testList.forEach { it.onPushReceived() }
    }

    fun onDiagnosticNotificationClicked() {
        testList.forEach { it.onNotificationClicked() }
    }
}
