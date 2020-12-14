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

package im.vector.app.features.login

import android.os.Bundle
import android.view.View
import butterknife.OnClick
import im.vector.app.R
import im.vector.app.core.utils.openUrlInChromeCustomTab
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
        loginServerChoiceMatrixOrg.isChecked = state.serverType == ServerType.MatrixOrg
    }

    private fun initTextViews() {
        loginServerChoiceEmsLearnMore.text = span {
            text = getString(R.string.login_server_modular_learn_more)
            textDecorationLine = "underline"
        }
    }

    @OnClick(R.id.loginServerChoiceEmsLearnMore)
    fun learnMore() {
        openUrlInChromeCustomTab(requireActivity(), null, EMS_LINK)
    }

    @OnClick(R.id.loginServerChoiceMatrixOrg)
    fun selectMatrixOrg() {
        loginViewModel.handle(LoginAction.UpdateServerType(ServerType.MatrixOrg))
    }

    @OnClick(R.id.loginServerChoiceEms)
    fun selectEMS() {
        loginViewModel.handle(LoginAction.UpdateServerType(ServerType.EMS))
    }

    @OnClick(R.id.loginServerChoiceOther)
    fun selectOther() {
        loginViewModel.handle(LoginAction.UpdateServerType(ServerType.Other))
    }

    @OnClick(R.id.loginServerIKnowMyIdSubmit)
    fun loginWithMatrixId() {
        loginViewModel.handle(LoginAction.UpdateSignMode(SignMode.SignInWithMatrixId))
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetHomeServerType)
    }

    override fun updateWithState(state: LoginViewState) {
        updateSelectedChoice(state)

        if (state.loginMode != LoginMode.Unknown) {
            // LoginFlow for matrix.org has been retrieved
            loginViewModel.handle(LoginAction.PostViewEvent(LoginViewEvents.OnLoginFlowRetrieved))
        }
    }
}
