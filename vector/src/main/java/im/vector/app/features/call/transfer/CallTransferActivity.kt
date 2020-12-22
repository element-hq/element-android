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
import android.view.View
import android.widget.Toast
import com.airbnb.mvrx.MvRx
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.app.core.utils.PERMISSIONS_FOR_MEMBERS_SEARCH
import im.vector.app.core.utils.PERMISSION_REQUEST_CODE_READ_CONTACTS
import im.vector.app.core.utils.allGranted
import im.vector.app.core.utils.checkPermissions
import im.vector.app.features.contactsbook.ContactsBookFragment
import im.vector.app.features.contactsbook.ContactsBookViewModel
import im.vector.app.features.contactsbook.ContactsBookViewState
import im.vector.app.features.userdirectory.UserListFragment
import im.vector.app.features.userdirectory.UserListFragmentArgs
import im.vector.app.features.userdirectory.UserListSharedAction
import im.vector.app.features.userdirectory.UserListSharedActionViewModel
import im.vector.app.features.userdirectory.UserListViewModel
import im.vector.app.features.userdirectory.UserListViewState
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class CallTransferArgs(val callId: String) : Parcelable

class CallTransferActivity : SimpleFragmentActivity(), CallTransferViewModel.Factory, UserListViewModel.Factory, ContactsBookViewModel.Factory {

    private lateinit var sharedActionViewModel: UserListSharedActionViewModel
    @Inject lateinit var userListViewModelFactory: UserListViewModel.Factory
    @Inject lateinit var callTransferViewModelFactory: CallTransferViewModel.Factory
    @Inject lateinit var contactsBookViewModelFactory: ContactsBookViewModel.Factory
    @Inject lateinit var errorFormatter: ErrorFormatter

    override fun injectWith(injector: ScreenComponent) {
        super.injectWith(injector)
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

        views.toolbar.visibility = View.GONE

        sharedActionViewModel = viewModelProvider.get(UserListSharedActionViewModel::class.java)
        sharedActionViewModel
                .observe()
                .subscribe { sharedAction ->
                    when (sharedAction) {
                        UserListSharedAction.Close                 -> finish()
                        UserListSharedAction.GoBack                -> onBackPressed()
                        UserListSharedAction.OpenPhoneBook         -> openPhoneBook()
                        // not exhaustive because it's a sharedAction
                        else                                       -> {
                        }
                    }
                }
                .disposeOnDestroy()
        if (isFirstCreation()) {
            addFragment(
                    R.id.container,
                    UserListFragment::class.java,
                    UserListFragmentArgs(
                            title = "Call transfer",
                            menuResId = -1
                    )
            )
        }
    }

    private fun openPhoneBook() {
        // Check permission first
        if (checkPermissions(PERMISSIONS_FOR_MEMBERS_SEARCH,
                        this,
                        PERMISSION_REQUEST_CODE_READ_CONTACTS,
                        0)) {
            addFragmentToBackstack(R.id.container, ContactsBookFragment::class.java)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (allGranted(grantResults)) {
            if (requestCode == PERMISSION_REQUEST_CODE_READ_CONTACTS) {
                doOnPostResume { addFragmentToBackstack(R.id.container, ContactsBookFragment::class.java) }
            }
        } else {
            Toast.makeText(baseContext, R.string.missing_permissions_error, Toast.LENGTH_SHORT).show()
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
