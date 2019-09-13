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
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.addFragment
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.features.disclaimer.showDisclaimerDialog
import javax.inject.Inject


class LoginActivity : VectorBaseActivity() {

    @Inject lateinit var loginViewModelFactory: LoginViewModel.Factory


    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getLayoutRes() = R.layout.activity_simple

    override fun initUiAndData() {
        if (isFirstCreation()) {
            addFragment(LoginFragment(), R.id.simpleFragmentContainer)
        }
    }


    override fun onResume() {
        super.onResume()

        showDisclaimerDialog(this)
    }


    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, LoginActivity::class.java)
        }

    }

}
