/*
 * Copyright 2018 New Vector Ltd
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
package im.vector.app.features.settings.troubleshoot

import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import kotlin.properties.Delegates

class NotificationTroubleshootTestManager(val fragment: Fragment) {

    val testList = ArrayList<TroubleshootTest>()
    var isCancelled = false

    var currentTestIndex by Delegates.observable(0) { _, _, _ ->
        statusListener?.invoke(this)
    }
    val adapter = NotificationTroubleshootRecyclerViewAdapter(testList)

    var statusListener: ((NotificationTroubleshootTestManager) -> Unit)? = null

    var diagStatus: TroubleshootTest.TestStatus by Delegates.observable(TroubleshootTest.TestStatus.NOT_STARTED) { _, _, _ ->
        statusListener?.invoke(this)
    }

    fun addTest(test: TroubleshootTest) {
        testList.add(test)
        test.manager = this
    }

    fun runDiagnostic() {
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
                                    troubleshootTest.perform()
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
            testList.firstOrNull()?.perform()
        }
    }

    fun retry() {
        for (test in testList) {
            test.cancel()
            test.description = null
            test.quickFix = null
            test.status = TroubleshootTest.TestStatus.NOT_STARTED
        }
        runDiagnostic()
    }

    fun cancel() {
        isCancelled = true
        for (test in testList) {
            test.cancel()
        }
    }

    companion object {
        const val REQ_CODE_FIX = 9099
    }
}
