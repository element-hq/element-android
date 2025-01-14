/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.terms

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.airbnb.mvrx.viewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.terms.TermsService

@AndroidEntryPoint
class ReviewTermsActivity : SimpleFragmentActivity() {

    private val reviewTermsViewModel: ReviewTermsViewModel by viewModel()

    override fun initUiAndData() {
        super.initUiAndData()

        if (isFirstCreation()) {
            replaceFragment(views.container, ReviewTermsFragment::class.java)
        }

        reviewTermsViewModel.termsArgs = intent.getParcelableExtraCompat(EXTRA_INFO) ?: error("Missing parameter")

        reviewTermsViewModel.observeViewEvents {
            when (it) {
                is ReviewTermsViewEvents.Loading -> Unit
                is ReviewTermsViewEvents.Failure -> {
                    MaterialAlertDialogBuilder(this)
                            .setMessage(errorFormatter.toHumanReadable(it.throwable))
                            .setPositiveButton(CommonStrings.ok) { _, _ ->
                                if (it.finish) {
                                    finish()
                                }
                            }
                            .show()
                    Unit
                }
                ReviewTermsViewEvents.Success -> {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }

    companion object {
        private const val EXTRA_INFO = "EXTRA_INFO"

        fun intent(context: Context, serviceType: TermsService.ServiceType, baseUrl: String, token: String?): Intent {
            return Intent(context, ReviewTermsActivity::class.java).also {
                it.putExtra(EXTRA_INFO, ServiceTermsArgs(serviceType, baseUrl, token))
            }
        }
    }
}
