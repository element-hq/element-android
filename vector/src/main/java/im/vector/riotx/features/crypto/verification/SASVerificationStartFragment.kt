/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.riotx.features.crypto.verification

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.transition.TransitionManager
import butterknife.BindView
import butterknife.OnClick
import im.vector.matrix.android.api.session.crypto.sas.OutgoingSasVerificationRequest
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.platform.VectorBaseFragment
import javax.inject.Inject

class SASVerificationStartFragment @Inject constructor(): VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_sas_verification_start

    private lateinit var viewModel: SasVerificationViewModel

    @BindView(R.id.rootLayout)
    lateinit var rootLayout: ViewGroup

    @BindView(R.id.sas_start_button)
    lateinit var startButton: Button

    @BindView(R.id.sas_start_button_loading)
    lateinit var startButtonLoading: ProgressBar

    @BindView(R.id.sas_verifying_keys)
    lateinit var loadingText: TextView

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = activityViewModelProvider.get(SasVerificationViewModel::class.java)
        viewModel.transactionState.observe(viewLifecycleOwner, Observer {
            val uxState = (viewModel.transaction as? OutgoingSasVerificationRequest)?.uxState
            when (uxState) {
                OutgoingSasVerificationRequest.UxState.WAIT_FOR_KEY_AGREEMENT -> {
                    // display loading
                    TransitionManager.beginDelayedTransition(this.rootLayout)
                    this.loadingText.isVisible = true
                    this.startButton.isInvisible = true
                    this.startButtonLoading.isVisible = true
                    this.startButtonLoading.animate()
                }
                OutgoingSasVerificationRequest.UxState.SHOW_SAS               -> {
                    viewModel.shortCodeReady()
                }
                OutgoingSasVerificationRequest.UxState.CANCELLED_BY_ME,
                OutgoingSasVerificationRequest.UxState.CANCELLED_BY_OTHER     -> {
                    viewModel.navigateCancel()
                }
                else                                                          -> {
                    TransitionManager.beginDelayedTransition(this.rootLayout)
                    this.loadingText.isVisible = false
                    this.startButton.isVisible = true
                    this.startButtonLoading.isVisible = false
                }
            }
        })
    }

    @OnClick(R.id.sas_start_button)
    fun doStart() {
        viewModel.beginSasKeyVerification()
    }

    @OnClick(R.id.sas_legacy_verification)
    fun doLegacy() {
        (requireActivity() as VectorBaseActivity).notImplemented()

        /*
        viewModel.session.crypto?.getDeviceInfo(viewModel.otherUserId ?: "", viewModel.otherDeviceId
                ?: "", object : SimpleApiCallback<MXDeviceInfo>() {
            override fun onSuccess(info: MXDeviceInfo?) {
                info?.let {

                    CommonActivityUtils.displayDeviceVerificationDialogLegacy(it, it.userId, viewModel.session, activity, object : YesNoListener {
                        override fun yes() {
                            viewModel.manuallyVerified()
                        }

                        override fun no() {

                        }
                    })
                }
            }
        })
        */
    }

    @OnClick(R.id.sas_cancel_button)
    fun doCancel() {
        // Transaction may be started, or not
        viewModel.cancelTransaction()
    }
}
