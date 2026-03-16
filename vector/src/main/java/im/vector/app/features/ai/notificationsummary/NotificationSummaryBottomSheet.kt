/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.ai.notificationsummary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NotificationSummaryBottomSheet : BottomSheetDialogFragment() {

    @Inject lateinit var notificationSummaryService: NotificationSummaryService

    var onDismiss: (() -> Unit)? = null
    var onMarkAllRead: (() -> Unit)? = null

    private var notificationsData: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationsData = arguments?.getStringArrayList(ARG_NOTIFICATIONS) ?: arrayListOf()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val padding = (16 * context.resources.displayMetrics.density).toInt()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val title = TextView(context).apply {
            text = getString(R.string.notification_summary_title)
            textSize = 18f
            setPadding(0, 0, 0, padding)
        }
        root.addView(title)

        val progressBar = ProgressBar(context).apply {
            isIndeterminate = true
        }
        root.addView(progressBar)

        val statusText = TextView(context).apply {
            text = getString(R.string.notification_summary_loading)
        }
        root.addView(statusText)

        val scrollView = ScrollView(context).apply {
            isVisible = false
            layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (250 * context.resources.displayMetrics.density).toInt()
            )
        }

        val summaryText = TextView(context).apply {
            textSize = 14f
            setPadding(0, padding / 2, 0, padding / 2)
        }
        scrollView.addView(summaryText)
        root.addView(scrollView)

        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            isVisible = false
        }

        val dismissButton = Button(context).apply {
            text = getString(R.string.notification_summary_dismiss)
            setOnClickListener {
                onDismiss?.invoke()
                dismissAllowingStateLoss()
            }
        }
        buttonsLayout.addView(dismissButton)

        val markReadButton = Button(context).apply {
            text = getString(R.string.notification_summary_mark_read)
            setOnClickListener {
                onMarkAllRead?.invoke()
                dismissAllowingStateLoss()
            }
        }
        buttonsLayout.addView(markReadButton)
        root.addView(buttonsLayout)

        // Parse notifications as pairs of room:message
        val notifPairs = notificationsData.map { line ->
            val parts = line.split("|", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else "" to line
        }

        Timber.d("TRANSLATION_DEBUG NotificationSummaryBottomSheet: generating summary for ${notifPairs.size} notifications")

        lifecycleScope.launch {
            try {
                val result = notificationSummaryService.generateSummary(notifPairs)
                progressBar.isVisible = false
                if (result != null) {
                    statusText.isVisible = false
                    summaryText.text = result
                    scrollView.isVisible = true
                    buttonsLayout.isVisible = true
                    Timber.d("TRANSLATION_DEBUG NotificationSummaryBottomSheet: summary generated successfully")
                } else {
                    statusText.text = getString(R.string.notification_summary_failed)
                    Timber.d("TRANSLATION_DEBUG NotificationSummaryBottomSheet: summary generation returned null")
                }
            } catch (e: Exception) {
                Timber.e(e, "TRANSLATION_DEBUG NotificationSummaryBottomSheet: summary generation failed")
                progressBar.isVisible = false
                statusText.text = getString(R.string.notification_summary_failed)
            }
        }

        return root
    }

    companion object {
        private const val ARG_NOTIFICATIONS = "arg_notifications"
        private const val TAG = "NotificationSummaryBottomSheet"

        fun show(
                fragmentManager: FragmentManager,
                notifications: List<String>,
                onDismiss: (() -> Unit)? = null,
                onMarkAllRead: (() -> Unit)? = null
        ) {
            // Avoid showing multiple instances
            if (fragmentManager.findFragmentByTag(TAG) != null) return

            val sheet = NotificationSummaryBottomSheet().apply {
                arguments = Bundle().apply { putStringArrayList(ARG_NOTIFICATIONS, ArrayList(notifications)) }
                this.onDismiss = onDismiss
                this.onMarkAllRead = onMarkAllRead
            }
            sheet.show(fragmentManager, TAG)
        }
    }
}
