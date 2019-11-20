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

package im.vector.riotx.features.login

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.transition.TransitionInflater
import butterknife.OnClick
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.utils.openUrlInExternalBrowser
import kotlinx.android.synthetic.main.fragment_login_server_selection.*
import me.gujun.android.span.span
import javax.inject.Inject

/**
 * In this screen, the user will choose between matrix.org, modular or other type of homeserver
 */
class LoginServerSelectionFragment @Inject constructor(
        private val errorFormatter: ErrorFormatter
) : AbstractLoginFragment() {

    override fun getLayoutResId() = R.layout.fragment_login_server_selection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateSelectedChoice()
        initTextViews()
    }

    private fun updateSelectedChoice() {
        loginViewModel.serverType.let {
            loginServerChoiceMatrixOrg.isChecked = it == ServerType.MatrixOrg
            loginServerChoiceModular.isChecked = it == ServerType.Modular
            loginServerChoiceOther.isChecked = it == ServerType.Other
        }
    }

    private fun initTextViews() {
        loginServerChoiceModularLearnMore.text = span {
            text = getString(R.string.login_server_modular_learn_more)
            textDecorationLine = "underline"
            onClick = {
                // TODO this does not work
                openUrlInExternalBrowser(requireActivity(), "https://example.org")
            }
        }
    }

    @OnClick(R.id.loginServerChoiceMatrixOrg)
    fun selectMatrixOrg() {
        if (loginServerChoiceMatrixOrg.isChecked) {
            // Consider this is a submit
            submit()
        } else {
            loginViewModel.handle(LoginAction.UpdateServerType(ServerType.MatrixOrg))
            updateSelectedChoice()
        }
    }

    @OnClick(R.id.loginServerChoiceModular)
    fun selectModular() {
        if (loginServerChoiceModular.isChecked) {
            // Consider this is a submit
            submit()
        } else {
            loginViewModel.handle(LoginAction.UpdateServerType(ServerType.Modular))
            updateSelectedChoice()
        }
    }

    @OnClick(R.id.loginServerChoiceOther)
    fun selectOther() {
        if (loginServerChoiceOther.isChecked) {
            // Consider this is a submit
            submit()
        } else {
            loginViewModel.handle(LoginAction.UpdateServerType(ServerType.Other))
            updateSelectedChoice()
        }
    }

    @OnClick(R.id.loginServerSubmit)
    fun submit() {
        if (loginViewModel.serverType == ServerType.MatrixOrg) {
            // Request login flow here
            loginViewModel.handle(LoginAction.UpdateHomeServer(getString(R.string.matrix_org_server_url)))
        } else {
            loginSharedActionViewModel.post(LoginNavigation.OnServerSelectionDone)
        }
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetHomeServerType)
    }

    override fun onRegistrationError(throwable: Throwable) {
        // Cannot happen here, but just in case
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(throwable))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    override fun invalidate() = withState(loginViewModel) {
        when (it.asyncHomeServerLoginFlowRequest) {
            is Fail    -> {
                // TODO Display error in a dialog?
            }
            is Success -> {
                // LoginFlow for matrix.org has been retrieved
                loginSharedActionViewModel.post(LoginNavigation.OnLoginFlowRetrieved)
            }
        }
    }
}
