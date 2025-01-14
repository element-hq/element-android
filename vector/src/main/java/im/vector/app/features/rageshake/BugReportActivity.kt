/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.databinding.ActivityBugReportBinding
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.extensions.tryOrNull
import timber.log.Timber

/**
 * Form to send a bug report.
 */
@AndroidEntryPoint
class BugReportActivity :
        VectorBaseActivity<ActivityBugReportBinding>(),
        VectorMenuProvider {

    override fun getBinding() = ActivityBugReportBinding.inflate(layoutInflater)

    private val viewModel: BugReportViewModel by viewModel()

    private var reportType: ReportType = ReportType.BUG_REPORT

    override fun initUiAndData() {
        setupToolbar(views.bugReportToolbar)
                .allowBack()
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
                supportActionBar?.setTitle(CommonStrings.title_activity_bug_report)
                views.bugReportButtonContactMe.isVisible = true
            }
            ReportType.SUGGESTION -> {
                supportActionBar?.setTitle(CommonStrings.send_suggestion)

                views.bugReportFirstText.setText(CommonStrings.send_suggestion_content)
                views.bugReportTextInputLayout.hint = getString(CommonStrings.send_suggestion_report_placeholder)
                views.bugReportButtonContactMe.isVisible = true

                hideBugReportOptions()
            }
            ReportType.SPACE_BETA_FEEDBACK -> {
                supportActionBar?.setTitle(CommonStrings.send_feedback_space_title)

                views.bugReportFirstText.setText(CommonStrings.send_feedback_space_info)
                views.bugReportTextInputLayout.hint = getString(CommonStrings.feedback)
                views.bugReportButtonContactMe.isVisible = true

                hideBugReportOptions()
            }
            ReportType.THREADS_BETA_FEEDBACK -> {
                supportActionBar?.setTitle(CommonStrings.send_feedback_threads_title)

                views.bugReportFirstText.setText(CommonStrings.send_feedback_threads_info)
                views.bugReportTextInputLayout.hint = getString(CommonStrings.feedback)
                views.bugReportButtonContactMe.isVisible = true

                hideBugReportOptions()
            }
            else -> {
                // other types not supported here
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

    override fun handlePrepareMenu(menu: Menu) {
        menu.findItem(R.id.ic_action_send_bug_report)?.let {
            val isValid = !views.bugReportMaskView.isVisible

            it.isEnabled = isValid
            it.icon?.alpha = if (isValid) 255 else 100
        }
    }

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ic_action_send_bug_report -> {
                if (views.bugReportEditText.text.toString().trim().length >= 10) {
                    sendBugReport()
                } else {
                    views.bugReportTextInputLayout.error = getString(CommonStrings.bug_report_error_too_short)
                }
                true
            }
            else -> false
        }
    }

    /**
     * Send the bug report.
     */
    private fun sendBugReport() = withState(viewModel) { state ->
        views.bugReportScrollview.alpha = 0.3f
        views.bugReportMaskView.isVisible = true

        invalidateOptionsMenu()

        views.bugReportProgressTextView.isVisible = true
        views.bugReportProgressTextView.text = getString(CommonStrings.send_bug_report_progress, "0")

        views.bugReportProgressView.isVisible = true
        views.bugReportProgressView.progress = 0

        bugReporter.sendBugReport(
                reportType,
                views.bugReportButtonIncludeLogs.isChecked,
                views.bugReportButtonIncludeCrashLogs.isChecked,
                views.bugReportButtonIncludeKeyShareHistory.isChecked,
                views.bugReportButtonIncludeScreenshot.isChecked,
                views.bugReportEditText.text.toString(),
                state.serverVersion,
                views.bugReportButtonContactMe.isChecked,
                null,
                object : BugReporter.IMXBugReportListener {
                    override fun onUploadFailed(reason: String?) {
                        try {
                            if (!reason.isNullOrEmpty()) {
                                when (reportType) {
                                    ReportType.BUG_REPORT -> {
                                        Toast.makeText(
                                                this@BugReportActivity,
                                                getString(CommonStrings.send_bug_report_failed, reason), Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    ReportType.SUGGESTION -> {
                                        Toast.makeText(
                                                this@BugReportActivity,
                                                getString(CommonStrings.send_suggestion_failed, reason), Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    ReportType.SPACE_BETA_FEEDBACK -> {
                                        Toast.makeText(
                                                this@BugReportActivity,
                                                getString(CommonStrings.feedback_failed, reason), Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    else -> {
                                        // nop
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
                        views.bugReportProgressTextView.text = getString(CommonStrings.send_bug_report_progress, myProgress.toString())
                    }

                    override fun onUploadSucceed(reportUrl: String?) {
                        try {
                            when (reportType) {
                                ReportType.BUG_REPORT -> {
                                    Toast.makeText(this@BugReportActivity, CommonStrings.send_bug_report_sent, Toast.LENGTH_LONG).show()
                                }
                                ReportType.SUGGESTION -> {
                                    Toast.makeText(this@BugReportActivity, CommonStrings.send_suggestion_sent, Toast.LENGTH_LONG).show()
                                }
                                ReportType.SPACE_BETA_FEEDBACK -> {
                                    Toast.makeText(this@BugReportActivity, CommonStrings.feedback_sent, Toast.LENGTH_LONG).show()
                                }
                                else -> {
                                    // nop
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
        views.bugReportScreenshotPreview.isVisible = bugReporter.screenshot != null
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // Ensure there is no crash status remaining, which will be sent later on by mistake
        bugReporter.deleteCrashFile()
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
