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
package im.vector.app.features.settings

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.rageshake.BugReporter
import im.vector.app.features.settings.troubleshoot.NotificationTroubleshootTestManager
import im.vector.app.features.settings.troubleshoot.TroubleshootTest
import im.vector.app.push.fcm.NotificationTroubleshootTestManagerFactory
import kotlinx.android.synthetic.main.fragment_settings_notifications_troubleshoot.*
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import javax.inject.Inject

class VectorSettingsNotificationsTroubleshootFragment @Inject constructor(
        private val bugReporter: BugReporter,
        private val testManagerFactory: NotificationTroubleshootTestManagerFactory
) : VectorBaseFragment() {

    private var testManager: NotificationTroubleshootTestManager? = null
    // members

    override fun getLayoutResId() = R.layout.fragment_settings_notifications_troubleshoot

    private var interactionListener: VectorSettingsFragmentInteractionListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context)
        troubleshoot_test_recycler_view.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(troubleshoot_test_recycler_view.context,
                layoutManager.orientation)
        troubleshoot_test_recycler_view.addItemDecoration(dividerItemDecoration)

        troubleshoot_summ_button.debouncedClicks {
            bugReporter.openBugReportScreen(requireActivity())
        }

        troubleshoot_run_button.debouncedClicks {
            testManager?.retry(testStartForActivityResult)
        }
        startUI()
    }

    private fun startUI() {
        toubleshoot_summ_description.text = getString(R.string.settings_troubleshoot_diagnostic_running_status, 0, 0)
        testManager = testManagerFactory.create(this)
        testManager?.statusListener = { troubleshootTestManager ->
            if (isAdded) {
                TransitionManager.beginDelayedTransition(troubleshoot_bottom_view)
                when (troubleshootTestManager.diagStatus) {
                    TroubleshootTest.TestStatus.NOT_STARTED      -> {
                        toubleshoot_summ_description.text = ""
                        troubleshoot_summ_button.visibility = View.GONE
                        troubleshoot_run_button.visibility = View.VISIBLE
                    }
                    TroubleshootTest.TestStatus.RUNNING,
                    TroubleshootTest.TestStatus.WAITING_FOR_USER -> {
                        val size = troubleshootTestManager.testListSize
                        val currentTestIndex = troubleshootTestManager.currentTestIndex
                        toubleshoot_summ_description.text = getString(
                                R.string.settings_troubleshoot_diagnostic_running_status,
                                currentTestIndex,
                                size
                        )
                        troubleshoot_summ_button.visibility = View.GONE
                        troubleshoot_run_button.visibility = View.GONE
                    }
                    TroubleshootTest.TestStatus.FAILED           -> {
                        // check if there are quick fixes
                        val hasQuickFix = testManager?.hasQuickFix().orFalse()
                        if (hasQuickFix) {
                            toubleshoot_summ_description.text = getString(R.string.settings_troubleshoot_diagnostic_failure_status_with_quickfix)
                        } else {
                            toubleshoot_summ_description.text = getString(R.string.settings_troubleshoot_diagnostic_failure_status_no_quickfix)
                        }
                        troubleshoot_summ_button.visibility = View.VISIBLE
                        troubleshoot_run_button.visibility = View.VISIBLE
                    }
                    TroubleshootTest.TestStatus.SUCCESS          -> {
                        toubleshoot_summ_description.text = getString(R.string.settings_troubleshoot_diagnostic_success_status)
                        troubleshoot_summ_button.visibility = View.VISIBLE
                        troubleshoot_run_button.visibility = View.VISIBLE
                    }
                }
            }
        }
        troubleshoot_test_recycler_view.adapter = testManager?.adapter
        testManager?.runDiagnostic(testStartForActivityResult)
    }

    override fun onDestroyView() {
        troubleshoot_test_recycler_view.cleanup()
        super.onDestroyView()
    }

    private val testStartForActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            retry()
        }
    }

    private fun retry() {
        testManager?.retry(testStartForActivityResult)
    }

    override fun onDetach() {
        testManager?.cancel()
        interactionListener = null
        super.onDetach()
    }

    override fun onResume() {
        super.onResume()
        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(R.string.settings_notification_troubleshoot)

        tryOrNull("Unable to register the receiver") {
            LocalBroadcastManager.getInstance(requireContext())
                    .registerReceiver(broadcastReceiverPush, IntentFilter(NotificationUtils.PUSH_ACTION))
        }
        tryOrNull("Unable to register the receiver") {
            LocalBroadcastManager.getInstance(requireContext())
                    .registerReceiver(broadcastReceiverNotification, IntentFilter(NotificationUtils.DIAGNOSTIC_ACTION))
        }
    }

    override fun onPause() {
        super.onPause()
        tryOrNull {
            LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(broadcastReceiverPush)
        }
        tryOrNull {
            LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(broadcastReceiverNotification)
        }
    }

    private val broadcastReceiverPush = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            testManager?.onDiagnosticPushReceived()
        }
    }

    private val broadcastReceiverNotification = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            testManager?.onDiagnosticNotificationClicked()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is VectorSettingsFragmentInteractionListener) {
            interactionListener = context
        }
    }
}
