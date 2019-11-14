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
import kotlinx.android.synthetic.main.fragment_login_server_selection.*
import me.gujun.android.span.span
import javax.inject.Inject

/**
 *
 */
class LoginServerSelectionFragment @Inject constructor() : AbstractLoginFragment() {

    override fun getLayoutResId() = R.layout.fragment_login_server_selection

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTextViews()
    }

    private fun updateSelectedChoice(serverType: ServerType) {
        loginServerChoiceMatrixOrg.isChecked = serverType == ServerType.MatrixOrg
        loginServerChoiceModular.isChecked = serverType == ServerType.Modular
        loginServerChoiceOther.isChecked = serverType == ServerType.Other
    }

    private fun initTextViews() {
        loginServerChoiceModularLearnMore.text = span {
            text = getString(R.string.login_server_modular_learn_more)
            textDecorationLine = "underline"
            onClick = {
                // TODO
            }
        }

    }

    @OnClick(R.id.loginServerChoiceMatrixOrg)
    fun selectMatrixOrg() {
        viewModel.handle(LoginAction.UpdateServerType(ServerType.MatrixOrg))
    }

    @OnClick(R.id.loginServerChoiceModular)
    fun selectModular() {
        viewModel.handle(LoginAction.UpdateServerType(ServerType.Modular))
    }

    @OnClick(R.id.loginServerChoiceOther)
    fun selectOther() {
        viewModel.handle(LoginAction.UpdateServerType(ServerType.Other))
    }

    @OnClick(R.id.loginServerSubmit)
    fun submit() {
        loginSharedActionViewModel.post(LoginNavigation.OnServerSelectionDone)
    }

    override fun invalidate() = withState(viewModel) {
        updateSelectedChoice(it.serverType)
    }
}
