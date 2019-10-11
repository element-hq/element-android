/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.login

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.viewModel
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.addFragment
import im.vector.riotx.core.extensions.addFragmentToBackstack
import im.vector.riotx.core.extensions.observeEvent
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.features.disclaimer.showDisclaimerDialog
import im.vector.riotx.features.home.HomeActivity
import javax.inject.Inject

class LoginActivity : VectorBaseActivity() {

    // Supported navigation actions for this Activity
    sealed class Navigation {
        object OpenSsoLoginFallback : Navigation()
        object GoBack : Navigation()
    }

    private val loginViewModel: LoginViewModel by viewModel()

    @Inject lateinit var loginViewModelFactory: LoginViewModel.Factory

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getLayoutRes() = R.layout.activity_simple

    override fun initUiAndData() {
        if (isFirstCreation()) {
            addFragment(LoginFragment(), R.id.simpleFragmentContainer)
        }

        // Get config extra
        val loginConfig = intent.getParcelableExtra<LoginConfig?>(EXTRA_CONFIG)
        if (loginConfig != null && isFirstCreation()) {
            loginViewModel.handle(LoginActions.InitWith(loginConfig))
        }

        loginViewModel.navigationLiveData.observeEvent(this) {
            when (it) {
                is Navigation.OpenSsoLoginFallback -> addFragmentToBackstack(LoginSsoFallbackFragment(), R.id.simpleFragmentContainer)
                is Navigation.GoBack               -> supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
        }

        loginViewModel.selectSubscribe(this, LoginViewState::asyncLoginAction) {
            if (it is Success) {
                val intent = HomeActivity.newIntent(this)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        showDisclaimerDialog(this)
    }

    companion object {
        private const val EXTRA_CONFIG = "EXTRA_CONFIG"

        fun newIntent(context: Context, loginConfig: LoginConfig?): Intent {
            return Intent(context, LoginActivity::class.java).apply {
                putExtra(EXTRA_CONFIG, loginConfig)
            }
        }
    }
}
