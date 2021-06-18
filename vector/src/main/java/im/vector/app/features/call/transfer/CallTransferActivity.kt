/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.call.transfer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.viewModel
import com.google.android.material.tabs.TabLayoutMediator
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityCallTransferBinding
import im.vector.app.features.contactsbook.ContactsBookViewModel
import im.vector.app.features.contactsbook.ContactsBookViewState
import im.vector.app.features.userdirectory.UserListViewModel
import im.vector.app.features.userdirectory.UserListViewState
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class CallTransferArgs(val callId: String) : Parcelable

private const val USER_LIST_FRAGMENT_TAG = "USER_LIST_FRAGMENT_TAG"

class CallTransferActivity : VectorBaseActivity<ActivityCallTransferBinding>(),
        CallTransferViewModel.Factory,
        UserListViewModel.Factory,
        ContactsBookViewModel.Factory {

    @Inject lateinit var userListViewModelFactory: UserListViewModel.Factory
    @Inject lateinit var callTransferViewModelFactory: CallTransferViewModel.Factory
    @Inject lateinit var contactsBookViewModelFactory: ContactsBookViewModel.Factory
    @Inject lateinit var errorFormatter: ErrorFormatter

    private lateinit var sectionsPagerAdapter: CallTransferPagerAdapter

    private val callTransferViewModel: CallTransferViewModel by viewModel()

    override fun getBinding() = ActivityCallTransferBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.vectorCoordinatorLayout

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun create(initialState: UserListViewState): UserListViewModel {
        return userListViewModelFactory.create(initialState)
    }

    override fun create(initialState: CallTransferViewState): CallTransferViewModel {
        return callTransferViewModelFactory.create(initialState)
    }

    override fun create(initialState: ContactsBookViewState): ContactsBookViewModel {
        return contactsBookViewModelFactory.create(initialState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        waitingView = views.waitingView.waitingView

        callTransferViewModel.observeViewEvents {
            when (it) {
                is CallTransferViewEvents.Dismiss        -> finish()
                CallTransferViewEvents.Loading           -> showWaitingView()
                is CallTransferViewEvents.FailToTransfer -> showSnackbar(getString(R.string.call_transfer_failure))
            }
        }

        sectionsPagerAdapter = CallTransferPagerAdapter(this)
        views.callTransferViewPager.adapter = sectionsPagerAdapter

        TabLayoutMediator(views.callTransferTabLayout, views.callTransferViewPager) { tab, position ->
            when (position) {
                CallTransferPagerAdapter.USER_LIST_INDEX -> tab.text = getString(R.string.call_transfer_users_tab_title)
                CallTransferPagerAdapter.DIAL_PAD_INDEX  -> tab.text = getString(R.string.call_dial_pad_title)
            }
        }.attach()
        configureToolbar(views.callTransferToolbar)
        views.callTransferToolbar.title = getString(R.string.call_transfer_title)
        setupConnectAction()
    }

    private fun setupConnectAction() {
        views.callTransferConnectAction.debouncedClicks {
            when (views.callTransferTabLayout.selectedTabPosition) {
                CallTransferPagerAdapter.USER_LIST_INDEX -> {
                    val selectedUser = sectionsPagerAdapter.userListFragment?.getCurrentState()?.getSelectedMatrixId()?.firstOrNull() ?: return@debouncedClicks
                    val action = CallTransferAction.ConnectWithUserId(views.callTransferConsultCheckBox.isChecked, selectedUser)
                    callTransferViewModel.handle(action)
                }
                CallTransferPagerAdapter.DIAL_PAD_INDEX  -> {
                    val phoneNumber = sectionsPagerAdapter.dialPadFragment?.getRawInput() ?: return@debouncedClicks
                    val action = CallTransferAction.ConnectWithPhoneNumber(views.callTransferConsultCheckBox.isChecked, phoneNumber)
                    callTransferViewModel.handle(action)
                }
            }
        }
    }

    companion object {

        fun newIntent(context: Context, callId: String): Intent {
            return Intent(context, CallTransferActivity::class.java).also {
                it.putExtra(MvRx.KEY_ARG, CallTransferArgs(callId))
            }
        }
    }
}
