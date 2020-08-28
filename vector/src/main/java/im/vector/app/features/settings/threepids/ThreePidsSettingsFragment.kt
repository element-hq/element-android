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

package im.vector.app.features.settings.threepids

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.dialogs.PromptPasswordDialog
import im.vector.app.core.dialogs.withColoredButton
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.isEmail
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.toast
import kotlinx.android.synthetic.main.fragment_generic_recycler.*
import org.matrix.android.sdk.api.session.identity.ThreePid
import javax.inject.Inject

class ThreePidsSettingsFragment @Inject constructor(
        private val viewModelFactory: ThreePidsSettingsViewModel.Factory,
        private val epoxyController: ThreePidsSettingsController
) :
        VectorBaseFragment(),
        ThreePidsSettingsViewModel.Factory by viewModelFactory,
        ThreePidsSettingsController.InteractionListener {

    private val viewModel: ThreePidsSettingsViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_generic_recycler

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.configureWith(epoxyController)
        epoxyController.interactionListener = this

        viewModel.observeViewEvents {
            when (it) {
                is ThreePidsSettingsViewEvents.Failure -> displayErrorDialog(it.throwable)
                ThreePidsSettingsViewEvents.RequestPassword -> askUserPassword()
            }.exhaustive
        }
    }

    private fun askUserPassword() {
        PromptPasswordDialog().show(requireActivity()) { password ->
            viewModel.handle(ThreePidsSettingsAction.AccountPassword(password))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView.cleanup()
        epoxyController.interactionListener = null
    }

    override fun onResume() {
        super.onResume()
        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(R.string.settings_emails_and_phone_numbers_title)
    }

    override fun invalidate() = withState(viewModel) { state ->
        if (state.isLoading) {
            showLoadingDialog()
        } else {
            dismissLoadingDialog()
        }
        epoxyController.setData(state)
    }

    override fun addEmail() {
        val inflater = requireActivity().layoutInflater
        val layout = inflater.inflate(R.layout.dialog_base_edit_text, null)

        val input = layout.findViewById<EditText>(R.id.editText)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.settings_add_email_address)
                .setView(layout)
                .setPositiveButton(R.string.ok) { _, _ ->
                    val email = input.text.toString()
                    doAddEmail(email)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun doAddEmail(email: String) {
        // Check that email is valid
        if (!email.isEmail()) {
            requireActivity().toast(R.string.auth_invalid_email)
            return
        }

        viewModel.handle(ThreePidsSettingsAction.AddThreePid(ThreePid.Email(email)))
    }

    override fun addMsisdn() {
        TODO("Not yet implemented")
    }

    override fun continueThreePid(threePid: ThreePid) {
        viewModel.handle(ThreePidsSettingsAction.ContinueThreePid(threePid))
    }

    override fun cancelThreePid(threePid: ThreePid) {
        viewModel.handle(ThreePidsSettingsAction.CancelThreePid(threePid))
    }

    override fun deleteThreePid(threePid: ThreePid) {
        AlertDialog.Builder(requireActivity())
                .setMessage(getString(R.string.settings_remove_three_pid_confirmation_content, threePid.value))
                .setPositiveButton(R.string.remove) { _, _ ->
                    viewModel.handle(ThreePidsSettingsAction.DeleteThreePid(threePid))
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
                .withColoredButton(DialogInterface.BUTTON_POSITIVE)
    }
}
