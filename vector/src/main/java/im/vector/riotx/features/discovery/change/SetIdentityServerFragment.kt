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
package im.vector.riotx.features.discovery.change

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import butterknife.BindView
import butterknife.OnTextChanged
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.textfield.TextInputLayout
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.discovery.DiscoverySharedViewModel
import javax.inject.Inject

class SetIdentityServerFragment @Inject constructor(
        val viewModelFactory: SetIdentityServerViewModel.Factory
) : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_set_identity_server

    override fun getMenuRes() = R.menu.menu_phone_number_addition

    @BindView(R.id.discovery_identity_server_enter_til)
    lateinit var mKeyInputLayout: TextInputLayout

    @BindView(R.id.discovery_identity_server_enter_edittext)
    lateinit var mKeyTextEdit: EditText

    @BindView(R.id.discovery_identity_server_loading)
    lateinit var mProgressBar: ProgressBar

    private val viewModel by fragmentViewModel(SetIdentityServerViewModel::class)

    lateinit var sharedViewModel: DiscoverySharedViewModel

    override fun invalidate() = withState(viewModel) { state ->
        if (state.isVerifyingServer) {
            mKeyTextEdit.isEnabled = false
            mProgressBar.isVisible = true
        } else {
            mKeyTextEdit.isEnabled = true
            mProgressBar.isVisible = false
        }
        val newText = state.newIdentityServer ?: ""
        if (!newText.equals(mKeyTextEdit.text.toString())) {
            mKeyTextEdit.setText(newText)
        }
        mKeyInputLayout.error = state.errorMessageId?.let { getString(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // TODO Create another menu
            R.id.action_add_phone_number -> {
                withState(viewModel) { state ->
                    if (!state.isVerifyingServer) {
                        viewModel.handle(SetIdentityServerAction.DoChangeServerName)
                    }
                }
                return true
            }
            else                         -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = activityViewModelProvider.get(DiscoverySharedViewModel::class.java)

        mKeyTextEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                withState(viewModel) { state ->
                    if (!state.isVerifyingServer) {
                        viewModel.handle(SetIdentityServerAction.DoChangeServerName)
                    }
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }


        viewModel.observeViewEvents {
            when (it) {
                is SetIdentityServerViewEvents.NoTerms       -> {
                    AlertDialog.Builder(requireActivity())
                            .setTitle(R.string.settings_discovery_no_terms_title)
                            .setMessage(R.string.settings_discovery_no_terms)
                            .setPositiveButton(R.string._continue) { _, _ ->
                                processIdentityServerChange()
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                }

                is SetIdentityServerViewEvents.TermsAccepted -> {
                    processIdentityServerChange()
                }

                is SetIdentityServerViewEvents.ShowTerms     -> {
                    /* TODO
                    ReviewTermsActivity.intent(requireContext(),
                            TermsManager.ServiceType.IdentityService,
                            SetIdentityServerViewModel.sanitatizeBaseURL(event.newIdentityServer),
                            null).also {
                        startActivityForResult(it, TERMS_REQUEST_CODE)
                    }
                    */
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        /* TODO
        if (requestCode == TERMS_REQUEST_CODE) {
            if (Activity.RESULT_OK == resultCode) {
                processIdentityServerChange()
            } else {
                //add some error?
            }
        }
         */
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun processIdentityServerChange() {
        withState(viewModel) { state ->
            if (state.newIdentityServer != null) {
                sharedViewModel.requestChangeToIdentityServer(state.newIdentityServer)
                parentFragmentManager.popBackStack()
            }
        }
    }

    @OnTextChanged(R.id.discovery_identity_server_enter_edittext)
    fun onTextEditChange(s: Editable?) {
        s?.toString()?.let { viewModel.handle(SetIdentityServerAction.UpdateServerName(it)) }
    }

    override fun onResume() {
        super.onResume()
        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(R.string.identity_server)
    }
}
