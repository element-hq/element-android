/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.databinding.FragmentLoginServerSelectionBinding
import im.vector.lib.strings.CommonStrings
import me.gujun.android.span.span

/**
 * In this screen, the user will choose between matrix.org, modular or other type of homeserver.
 */
@AndroidEntryPoint
class LoginServerSelectionFragment :
        AbstractLoginFragment<FragmentLoginServerSelectionBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginServerSelectionBinding {
        return FragmentLoginServerSelectionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        initTextViews()
    }

    private fun initViews() {
        views.loginServerChoiceEmsLearnMore.debouncedClicks { learnMore() }
        views.loginServerChoiceMatrixOrg.debouncedClicks { selectMatrixOrg() }
        views.loginServerChoiceEms.debouncedClicks { selectEMS() }
        views.loginServerChoiceOther.debouncedClicks { selectOther() }
        views.loginServerIKnowMyIdSubmit.debouncedClicks { loginWithMatrixId() }
    }

    private fun updateSelectedChoice(state: LoginViewState) {
        views.loginServerChoiceMatrixOrg.isChecked = state.serverType == ServerType.MatrixOrg
    }

    private fun initTextViews() {
        views.loginServerChoiceEmsLearnMore.text = span {
            text = getString(CommonStrings.login_server_modular_learn_more)
            textDecorationLine = "underline"
        }
    }

    private fun learnMore() {
        openUrlInChromeCustomTab(requireActivity(), null, EMS_LINK)
    }

    private fun selectMatrixOrg() {
        loginViewModel.handle(LoginAction.UpdateServerType(ServerType.MatrixOrg))
    }

    private fun selectEMS() {
        loginViewModel.handle(LoginAction.UpdateServerType(ServerType.EMS))
    }

    private fun selectOther() {
        loginViewModel.handle(LoginAction.UpdateServerType(ServerType.Other))
    }

    private fun loginWithMatrixId() {
        loginViewModel.handle(LoginAction.UpdateSignMode(SignMode.SignInWithMatrixId))
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetHomeServerType)
    }

    override fun updateWithState(state: LoginViewState) {
        updateSelectedChoice(state)
    }
}
