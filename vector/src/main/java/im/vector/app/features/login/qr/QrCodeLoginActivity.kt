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

@AndroidEntryPoint
class QrCodeLoginActivity : SimpleFragmentActivity() {

    private val viewModel: QrCodeLoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views.toolbar.visibility = View.GONE

        val qrCodeLoginArgs: QrCodeLoginArgs? = intent?.extras?.getParcelable(Mavericks.KEY_ARG)
        if (isFirstCreation()) {
            if (qrCodeLoginArgs?.loginType == QrCodeLoginType.LOGIN) {
                addFragment(
                        views.container,
                        QrCodeLoginInstructionsFragment::class.java,
                        qrCodeLoginArgs
                )
            }
        }

        observeViewEvents()
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents {
            when (it) {
                QrCodeLoginViewEvents.NavigateToStatusScreen -> handleNavigateToStatusScreen()
            }
        }
    }

    private fun handleNavigateToStatusScreen() {
        addFragment(
                views.container,
                QrCodeLoginStatusFragment::class.java
        )
    }

    companion object {

        fun getIntent(context: Context, qrCodeLoginArgs: QrCodeLoginArgs): Intent {
            return Intent(context, QrCodeLoginActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, qrCodeLoginArgs)
            }
        }
    }
}
