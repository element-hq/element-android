/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.ai.reformulate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.features.translation.TranslationService
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReformulationBottomSheet : BottomSheetDialogFragment() {

    @Inject lateinit var translationService: TranslationService

    private var originalText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        originalText = arguments?.getString(ARG_TEXT) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val padding = (16 * context.resources.displayMetrics.density).toInt()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val title = TextView(context).apply {
            text = getString(R.string.reformulate_title)
            textSize = 18f
            setPadding(0, 0, 0, padding)
        }
        root.addView(title)

        val progressBar = ProgressBar(context).apply {
            isVisible = false
            isIndeterminate = true
        }

        val statusText = TextView(context).apply {
            isVisible = false
        }

        val options = listOf(
                getString(R.string.reformulate_formal) to "Rewrite the following message in a formal, professional tone. Reply ONLY with the rewritten message.",
                getString(R.string.reformulate_casual) to "Rewrite the following message in a casual, friendly tone. Reply ONLY with the rewritten message.",
                getString(R.string.reformulate_concise) to "Rewrite the following message to be more concise and to the point. Reply ONLY with the rewritten message.",
                getString(R.string.reformulate_fix_grammar) to "Fix the grammar and spelling of the following message. Reply ONLY with the corrected message.",
        )

        options.forEach { (label, prompt) ->
            val button = TextView(context).apply {
                text = label
                textSize = 16f
                setPadding(padding, padding / 2, padding, padding / 2)
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener {
                    progressBar.isVisible = true
                    statusText.isVisible = true
                    statusText.text = getString(R.string.reformulate_loading)
                    lifecycleScope.launch {
                        val result = translationService.complete(prompt, originalText)
                        if (result != null) {
                            parentFragmentManager.setFragmentResult(
                                    REQUEST_KEY,
                                    bundleOf(RESULT_TEXT to result)
                            )
                            dismissAllowingStateLoss()
                        } else {
                            statusText.text = getString(R.string.reformulate_failed)
                            progressBar.isVisible = false
                        }
                    }
                }
            }
            root.addView(button)
        }

        root.addView(progressBar)
        root.addView(statusText)

        return root
    }

    companion object {
        private const val ARG_TEXT = "arg_text"
        private const val TAG = "ReformulationBottomSheet"
        const val REQUEST_KEY = "reformulation_result"
        const val RESULT_TEXT = "text"

        fun show(fragmentManager: FragmentManager, text: String) {
            val sheet = ReformulationBottomSheet().apply {
                arguments = Bundle().apply { putString(ARG_TEXT, text) }
            }
            sheet.show(fragmentManager, TAG)
        }
    }
}
