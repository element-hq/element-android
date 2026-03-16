/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.ai.summary

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
import im.vector.app.core.utils.copyToClipboard
import im.vector.app.features.translation.TranslationService
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConversationSummaryBottomSheet : BottomSheetDialogFragment() {

    @Inject lateinit var translationService: TranslationService

    private var messagesText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messagesText = arguments?.getString(ARG_MESSAGES) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val padding = (16 * context.resources.displayMetrics.density).toInt()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val title = TextView(context).apply {
            text = getString(R.string.summary_title)
            textSize = 18f
            setPadding(0, 0, 0, padding)
        }
        root.addView(title)

        val progressBar = ProgressBar(context).apply {
            isIndeterminate = true
        }
        root.addView(progressBar)

        val statusText = TextView(context).apply {
            text = getString(R.string.summary_loading)
        }
        root.addView(statusText)

        val scrollView = ScrollView(context).apply {
            isVisible = false
            layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (300 * context.resources.displayMetrics.density).toInt()
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

        val copyButton = Button(context).apply {
            text = getString(R.string.summary_copy)
            setOnClickListener {
                copyToClipboard(requireContext(), summaryText.text.toString())
            }
        }
        buttonsLayout.addView(copyButton)

        val closeButton = Button(context).apply {
            text = getString(R.string.summary_close)
            setOnClickListener { dismissAllowingStateLoss() }
        }
        buttonsLayout.addView(closeButton)
        root.addView(buttonsLayout)

        lifecycleScope.launch {
            val result = translationService.complete(
                    "You are a conversation summarizer. Summarize the following conversation concisely in the user's language. " +
                            "Focus on key topics, decisions, and action items. Be brief but comprehensive.",
                    messagesText
            )
            progressBar.isVisible = false
            if (result != null) {
                statusText.isVisible = false
                summaryText.text = result
                scrollView.isVisible = true
                buttonsLayout.isVisible = true
            } else {
                statusText.text = getString(R.string.summary_failed)
            }
        }

        return root
    }

    companion object {
        private const val ARG_MESSAGES = "arg_messages"
        private const val TAG = "ConversationSummaryBottomSheet"

        fun show(fragmentManager: FragmentManager, messagesText: String) {
            val sheet = ConversationSummaryBottomSheet().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGES, messagesText) }
            }
            sheet.show(fragmentManager, TAG)
        }
    }
}
