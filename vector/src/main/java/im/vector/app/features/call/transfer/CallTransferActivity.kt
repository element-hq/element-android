/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.transfer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityCallTransferBinding
import im.vector.lib.core.utils.compat.getParcelableCompat
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize

@Parcelize
data class CallTransferArgs(val callId: String) : Parcelable

private const val USER_LIST_FRAGMENT_TAG = "USER_LIST_FRAGMENT_TAG"

@AndroidEntryPoint
class CallTransferActivity : VectorBaseActivity<ActivityCallTransferBinding>() {

    private lateinit var sectionsPagerAdapter: CallTransferPagerAdapter

    private val callTransferViewModel: CallTransferViewModel by viewModel()

    override fun getBinding() = ActivityCallTransferBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.vectorCoordinatorLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        waitingView = views.waitingView.waitingView

        callTransferViewModel.observeViewEvents {
            when (it) {
                is CallTransferViewEvents.Complete -> handleComplete()
            }
        }

        sectionsPagerAdapter = CallTransferPagerAdapter(this, vectorLocale)
        views.callTransferViewPager.adapter = sectionsPagerAdapter

        TabLayoutMediator(views.callTransferTabLayout, views.callTransferViewPager) { tab, position ->
            when (position) {
                CallTransferPagerAdapter.USER_LIST_INDEX -> tab.text = getString(CommonStrings.call_transfer_users_tab_title)
                CallTransferPagerAdapter.DIAL_PAD_INDEX -> tab.text = getString(CommonStrings.call_dial_pad_title)
            }
        }.attach()
        setupToolbar(views.callTransferToolbar)
                .allowBack()
        setupConnectAction()
    }

    private fun setupConnectAction() {
        views.callTransferConnectAction.debouncedClicks {
            when (views.callTransferTabLayout.selectedTabPosition) {
                CallTransferPagerAdapter.USER_LIST_INDEX -> {
                    val selectedUser = sectionsPagerAdapter.userListFragment?.getCurrentState()?.getSelectedMatrixId()?.firstOrNull() ?: return@debouncedClicks
                    val result = CallTransferResult.ConnectWithUserId(views.callTransferConsultCheckBox.isChecked, selectedUser)
                    handleComplete(result)
                }
                CallTransferPagerAdapter.DIAL_PAD_INDEX -> {
                    val phoneNumber = sectionsPagerAdapter.dialPadFragment?.getRawInput() ?: return@debouncedClicks
                    val result = CallTransferResult.ConnectWithPhoneNumber(views.callTransferConsultCheckBox.isChecked, phoneNumber)
                    handleComplete(result)
                }
            }
        }
    }

    private fun handleComplete(callTransferResult: CallTransferResult? = null) {
        if (callTransferResult != null) {
            val intent = Intent().apply {
                putExtra(EXTRA_TRANSFER_RESULT, callTransferResult)
            }
            setResult(RESULT_OK, intent)
        } else {
            setResult(RESULT_OK)
        }
        finish()
    }

    companion object {
        private const val EXTRA_TRANSFER_RESULT = "EXTRA_TRANSFER_RESULT"

        fun newIntent(context: Context, callId: String): Intent {
            return Intent(context, CallTransferActivity::class.java).also {
                it.putExtra(Mavericks.KEY_ARG, CallTransferArgs(callId))
            }
        }

        fun getCallTransferResult(intent: Intent?): CallTransferResult? {
            return intent?.extras?.getParcelableCompat(EXTRA_TRANSFER_RESULT)
        }
    }
}
