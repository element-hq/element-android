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

package im.vector.riotx.features.phonebook

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.jakewharton.rxbinding3.widget.checkedChanges
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.matrix.android.api.session.identity.ThreePid
import im.vector.matrix.android.api.session.user.model.User
import im.vector.riotx.R
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.extensions.hideKeyboard
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.userdirectory.UserDirectoryAction
import im.vector.riotx.features.userdirectory.UserDirectorySharedAction
import im.vector.riotx.features.userdirectory.UserDirectorySharedActionViewModel
import im.vector.riotx.features.userdirectory.UserDirectoryViewModel
import kotlinx.android.synthetic.main.fragment_phonebook.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PhoneBookFragment @Inject constructor(
        val phoneBookViewModelFactory: PhoneBookViewModel.Factory,
        private val phoneBookController: PhoneBookController
) : VectorBaseFragment(), PhoneBookController.Callback {

    override fun getLayoutResId() = R.layout.fragment_phonebook
    private val viewModel: UserDirectoryViewModel by activityViewModel()

    // Use activityViewModel to avoid loading several times the data
    private val phoneBookViewModel: PhoneBookViewModel by activityViewModel()

    private lateinit var sharedActionViewModel: UserDirectorySharedActionViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(UserDirectorySharedActionViewModel::class.java)
        setupRecyclerView()
        setupFilterView()
        setupOnlyBoundContactsView()
        setupCloseView()
    }

    private fun setupOnlyBoundContactsView() {
        phoneBookOnlyBoundContacts.checkedChanges()
                .subscribe {
                    phoneBookViewModel.handle(PhoneBookAction.OnlyBoundContacts(it))
                }
                .disposeOnDestroyView()
    }

    private fun setupFilterView() {
        phoneBookFilter
                .textChanges()
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    phoneBookViewModel.handle(PhoneBookAction.FilterWith(it.toString()))
                }
                .disposeOnDestroyView()
    }

    override fun onDestroyView() {
        phoneBookRecyclerView.cleanup()
        phoneBookController.callback = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        phoneBookController.callback = this
        phoneBookRecyclerView.configureWith(phoneBookController)
    }

    private fun setupCloseView() {
        phoneBookClose.debouncedClicks {
            sharedActionViewModel.post(UserDirectorySharedAction.GoBack)
        }
    }

    override fun invalidate() = withState(phoneBookViewModel) { state ->
        phoneBookOnlyBoundContacts.isVisible = state.isBoundRetrieved
        phoneBookController.setData(state)
    }

    override fun onMatrixIdClick(matrixId: String) {
        view?.hideKeyboard()
        viewModel.handle(UserDirectoryAction.SelectUser(User(matrixId)))
        sharedActionViewModel.post(UserDirectorySharedAction.GoBack)
    }

    override fun onThreePidClick(threePid: ThreePid) {
        view?.hideKeyboard()
        viewModel.handle(UserDirectoryAction.SelectThreePid(threePid))
        sharedActionViewModel.post(UserDirectorySharedAction.GoBack)
    }
}
