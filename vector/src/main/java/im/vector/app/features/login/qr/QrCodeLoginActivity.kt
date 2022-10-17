/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.login.qr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.lib.core.utils.compat.getParcelableCompat
import timber.log.Timber

@AndroidEntryPoint
class QrCodeLoginActivity : SimpleFragmentActivity() {

    private val viewModel: QrCodeLoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views.toolbar.visibility = View.GONE

        val qrCodeLoginArgs: QrCodeLoginArgs? = intent?.extras?.getParcelableCompat(Mavericks.KEY_ARG)
        if (isFirstCreation()) {
            when (qrCodeLoginArgs?.loginType) {
                QrCodeLoginType.LOGIN -> {
                    showInstructionsFragment(qrCodeLoginArgs)
                }
                QrCodeLoginType.LINK_A_DEVICE -> {
                    if (qrCodeLoginArgs.showQrCodeImmediately) {
                        handleNavigateToShowQrCodeScreen()
                    } else {
                        showInstructionsFragment(qrCodeLoginArgs)
                    }
                }
                null -> {
                    Timber.i("QrCodeLoginArgs is null. This is not expected.")
                    finish()
                    return
                }
            }
        }

        observeViewEvents()
    }

    private fun showInstructionsFragment(qrCodeLoginArgs: QrCodeLoginArgs) {
        addFragment(
                views.container,
                QrCodeLoginInstructionsFragment::class.java,
                qrCodeLoginArgs,
                tag = FRAGMENT_QR_CODE_INSTRUCTIONS_TAG
        )
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents {
            when (it) {
                QrCodeLoginViewEvents.NavigateToStatusScreen -> handleNavigateToStatusScreen()
                QrCodeLoginViewEvents.NavigateToShowQrCodeScreen -> handleNavigateToShowQrCodeScreen()
            }
        }
    }

    private fun handleNavigateToShowQrCodeScreen() {
        addFragment(
                views.container,
                QrCodeLoginShowQrCodeFragment::class.java,
                tag = FRAGMENT_SHOW_QR_CODE_TAG
        )
    }

    private fun handleNavigateToStatusScreen() {
        addFragment(
                views.container,
                QrCodeLoginStatusFragment::class.java,
                tag = FRAGMENT_QR_CODE_STATUS_TAG
        )
    }

    companion object {

        private const val FRAGMENT_QR_CODE_INSTRUCTIONS_TAG = "FRAGMENT_QR_CODE_INSTRUCTIONS_TAG"
        private const val FRAGMENT_SHOW_QR_CODE_TAG = "FRAGMENT_SHOW_QR_CODE_TAG"
        private const val FRAGMENT_QR_CODE_STATUS_TAG = "FRAGMENT_QR_CODE_STATUS_TAG"

        fun getIntent(context: Context, qrCodeLoginArgs: QrCodeLoginArgs): Intent {
            return Intent(context, QrCodeLoginActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, qrCodeLoginArgs)
            }
        }
    }
}
