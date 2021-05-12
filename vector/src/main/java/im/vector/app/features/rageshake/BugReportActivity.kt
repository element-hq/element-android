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

package im.vector.app.features.rageshake

import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityBugReportBinding
import org.matrix.android.sdk.api.extensions.tryOrNull

import timber.log.Timber
import javax.inject.Inject

/**
 * Form to send a bug report
 */
class BugReportActivity : VectorBaseActivity<ActivityBugReportBinding>() {

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getBinding() = ActivityBugReportBinding.inflate(layoutInflater)

    @Inject lateinit var bugReportViewModelFactory: BugReportViewModel.Factory

    private val viewModel: BugReportViewModel by viewModel()

    private var reportType: ReportType = ReportType.BUG_REPORT

    override fun initUiAndData() {
        configureToolbar(views.bugReportToolbar)
        setupViews()

        if (bugReporter.screenshot != null) {
            views.bugReportScreenshotPreview.setImageBitmap(bugReporter.screenshot)
        } else {
            views.bugReportScreenshotPreview.isVisible = false
            views.bugReportButtonIncludeScreenshot.isChecked = false
            views.bugReportButtonIncludeScreenshot.isEnabled = false
        }

        reportType = intent.getStringExtra(REPORT_TYPE_EXTRA)?.let {
            tryOrNull { ReportType.valueOf(it) }
        } ?: ReportType.BUG_REPORT

        // Default screen is for bug report, so modify it for suggestion
        when (reportType) {
            ReportType.BUG_REPORT -> {
                supportActionBar?.setTitle(R.string.title_activity_bug_report)
                views.bugReportButtonContactMe.isVisible = true
            }
            ReportType.SUGGESTION -> {
                supportActionBar?.setTitle(R.string.send_suggestion)

                views.bugReportFirstText.setText(R.string.send_suggestion_content)
                views.bugReportTextInputLayout.hint = getString(R.string.send_suggestion_report_placeholder)
                views.bugReportButtonContactMe.isVisible = true

                hideBugReportOptions()
            }
            ReportType.SPACE_BETA_FEEDBACK -> {
                supportActionBar?.setTitle(R.string.send_feedback_space_title)

                views.bugReportFirstText.setText(R.string.send_feedback_space_info)
                views.bugReportTextInputLayout.hint = getString(R.string.feedback)
                views.bugReportButtonContactMe.isVisible = true

                hideBugReportOptions()
            }
        }
    }

    private fun hideBugReportOptions() {
        views.bugReportLogsDescription.isVisible = false

        views.bugReportButtonIncludeLogs.isChecked = false
        views.bugReportButtonIncludeLogs.isVisible = false

        views.bugReportButtonIncludeCrashLogs.isChecked = false
        views.bugReportButtonIncludeCrashLogs.isVisible = false

        views.bugReportButtonIncludeKeyShareHistory.isChecked = false
        views.bugReportButtonIncludeKeyShareHistory.isVisible = false
    }

    private fun setupViews() {
        views.bugReportEditText.doOnTextChanged { _, _, _, _ -> textChanged() }
        views.bugReportButtonIncludeScreenshot.setOnCheckedChangeListener { _, _ -> onSendScreenshotChanged() }
    }

    override fun getMenuRes() = R.menu.bug_report

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.ic_action_send_bug_report)?.let {
            val isValid = !views.bugReportMaskView.isVisible

            it.isEnabled = isValid
            it.icon.alpha = if (isValid) 255 else 100
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.ic_action_send_bug_report -> {
                if (views.bugReportEditText.text.toString().trim().length >= 10) {
                    sendBugReport()
                } else {
                    views.bugReportTextInputLayout.error = getString(R.string.bug_report_error_too_short)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Send the bug report
     */
    private fun sendBugReport() = withState(viewModel) { state ->
        views.bugReportScrollview.alpha = 0.3f
        views.bugReportMaskView.isVisible = true

        invalidateOptionsMenu()

        views.bugReportProgressTextView.isVisible = true
        views.bugReportProgressTextView.text = getString(R.string.send_bug_report_progress, "0")

        views.bugReportProgressView.isVisible = true
        views.bugReportProgressView.progress = 0

        bugReporter.sendBugReport(this,
                reportType,
                views.bugReportButtonIncludeLogs.isChecked,
                views.bugReportButtonIncludeCrashLogs.isChecked,
                views.bugReportButtonIncludeKeyShareHistory.isChecked,
                views.bugReportButtonIncludeScreenshot.isChecked,
                views.bugReportEditText.text.toString(),
                state.serverVersion,
                views.bugReportButtonContactMe.isChecked,
                object : BugReporter.IMXBugReportListener {
                    override fun onUploadFailed(reason: String?) {
                        try {
                            if (!reason.isNullOrEmpty()) {
                                when (reportType) {
                                    ReportType.BUG_REPORT          -> {
                                        Toast.makeText(this@BugReportActivity,
                                                getString(R.string.send_bug_report_failed, reason), Toast.LENGTH_LONG).show()
                                    }
                                    ReportType.SUGGESTION          -> {
                                        Toast.makeText(this@BugReportActivity,
                                                getString(R.string.send_suggestion_failed, reason), Toast.LENGTH_LONG).show()
                                    }
                                    ReportType.SPACE_BETA_FEEDBACK -> {
                                        Toast.makeText(this@BugReportActivity,
                                                getString(R.string.feedback_failed, reason), Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "## onUploadFailed() : failed to display the toast")
                        }

                        views.bugReportMaskView.isVisible = false
                        views.bugReportProgressView.isVisible = false
                        views.bugReportProgressTextView.isVisible = false
                        views.bugReportScrollview.alpha = 1.0f

                        invalidateOptionsMenu()
                    }

                    override fun onUploadCancelled() {
                        onUploadFailed(null)
                    }

                    override fun onProgress(progress: Int) {
                        val myProgress = progress.coerceIn(0, 100)

                        views.bugReportProgressView.progress = myProgress
                        views.bugReportProgressTextView.text = getString(R.string.send_bug_report_progress, myProgress.toString())
                    }

                    override fun onUploadSucceed() {
                        try {
                            when (reportType) {
                                ReportType.BUG_REPORT          -> {
                                    Toast.makeText(this@BugReportActivity, R.string.send_bug_report_sent, Toast.LENGTH_LONG).show()
                                }
                                ReportType.SUGGESTION          -> {
                                    Toast.makeText(this@BugReportActivity, R.string.send_suggestion_sent, Toast.LENGTH_LONG).show()
                                }
                                ReportType.SPACE_BETA_FEEDBACK -> {
                                    Toast.makeText(this@BugReportActivity, R.string.feedback_sent, Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "## onUploadSucceed() : failed to dismiss the toast")
                        }

                        try {
                            finish()
                        } catch (e: Exception) {
                            Timber.e(e, "## onUploadSucceed() : failed to dismiss the dialog")
                        }
                    }
                })
    }

    /* ==========================================================================================
     * UI Event
     * ========================================================================================== */

    private fun textChanged() {
        views.bugReportTextInputLayout.error = null
    }

    private fun onSendScreenshotChanged() {
        views.bugReportScreenshotPreview.isVisible = views.bugReportButtonIncludeScreenshot.isChecked && bugReporter.screenshot != null
    }

    override fun onBackPressed() {
        // Ensure there is no crash status remaining, which will be sent later on by mistake
        bugReporter.deleteCrashFile(this)

        super.onBackPressed()
    }

    companion object {
        private const val REPORT_TYPE_EXTRA = "REPORT_TYPE_EXTRA"

        fun intent(context: Context, reportType: ReportType): Intent {
            return Intent(context, BugReportActivity::class.java).apply {
                putExtra(REPORT_TYPE_EXTRA, reportType.name)
            }
        }
    }
}
