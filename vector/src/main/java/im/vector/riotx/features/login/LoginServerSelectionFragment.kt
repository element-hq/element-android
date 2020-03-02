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

import android.os.Bundle
import android.view.View
import butterknife.OnClick
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.utils.openUrlInExternalBrowser
import kotlinx.android.synthetic.main.fragment_login_server_selection.*
import me.gujun.android.span.span
import javax.inject.Inject

/**
 * In this screen, the user will choose between matrix.org, modular or other type of homeserver
 */
class LoginServerSelectionFragment @Inject constructor() : AbstractLoginFragment() {

    override fun getLayoutResId() = R.layout.fragment_login_server_selection

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTextViews()
    }

    private fun updateSelectedChoice(state: LoginViewState) {
        state.serverType.let {
            loginServerChoiceMatrixOrg.isChecked = it == ServerType.MatrixOrg
            loginServerChoiceModular.isChecked = it == ServerType.Modular
            loginServerChoiceOther.isChecked = it == ServerType.Other
        }
    }

    private fun initTextViews() {
        loginServerChoiceModularLearnMore.text = span {
            text = getString(R.string.login_server_modular_learn_more)
            textDecorationLine = "underline"
        }
    }

    @OnClick(R.id.loginServerChoiceModularLearnMore)
    fun learMore() {
        openUrlInExternalBrowser(requireActivity(), MODULAR_LINK)
    }

    @OnClick(R.id.loginServerChoiceMatrixOrg)
    fun selectMatrixOrg() {
        if (loginServerChoiceMatrixOrg.isChecked) {
            // Consider this is a submit
            submit()
        } else {
            loginViewModel.handle(LoginAction.UpdateServerType(ServerType.MatrixOrg))
        }
    }

    @OnClick(R.id.loginServerChoiceModular)
    fun selectModular() {
        if (loginServerChoiceModular.isChecked) {
            // Consider this is a submit
            submit()
        } else {
            loginViewModel.handle(LoginAction.UpdateServerType(ServerType.Modular))
        }
    }

    @OnClick(R.id.loginServerChoiceOther)
    fun selectOther() {
        if (loginServerChoiceOther.isChecked) {
            // Consider this is a submit
            submit()
        } else {
            loginViewModel.handle(LoginAction.UpdateServerType(ServerType.Other))
        }
    }

    @OnClick(R.id.loginServerSubmit)
    fun submit() = withState(loginViewModel) { state ->
        if (state.serverType == ServerType.MatrixOrg) {
            // Request login flow here
            loginViewModel.handle(LoginAction.UpdateHomeServer(getString(R.string.matrix_org_server_url)))
        } else {
            loginSharedActionViewModel.post(LoginNavigation.OnServerSelectionDone)
        }
    }

    @OnClick(R.id.loginServerIKnowMyIdSubmit)
    fun loginWithMatrixId() {
        loginViewModel.handle(LoginAction.UpdateSignMode(SignMode.SignInWithMatrixId))
        loginSharedActionViewModel.post(LoginNavigation.OnSignModeSelected)
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetHomeServerType)
    }

    override fun updateWithState(state: LoginViewState) {
        updateSelectedChoice(state)

        if (state.loginMode != LoginMode.Unknown) {
            // LoginFlow for matrix.org has been retrieved
            loginSharedActionViewModel.post(LoginNavigation.OnLoginFlowRetrieved)
        }
    }
}
