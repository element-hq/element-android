/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.ftue

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.lazyViewModel
import im.vector.app.core.platform.ToolbarConfigurable
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.lifecycleAwareLazy
import im.vector.app.databinding.ActivityLoginBinding
import im.vector.app.features.login.LoginConfig
import im.vector.app.features.pin.UnlockedActivity
import javax.inject.Inject

@AndroidEntryPoint
class FTUEActivity : VectorBaseActivity<ActivityLoginBinding>(), ToolbarConfigurable, UnlockedActivity {

    private val ftueVariant by lifecycleAwareLazy {
        ftueVariantFactory.create(this, ftueViewModel = lazyViewModel(), loginViewModel2 = lazyViewModel())
    }

    @Inject lateinit var ftueVariantFactory: FTUEVariantFactory

    override fun getBinding() = ActivityLoginBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun configure(toolbar: MaterialToolbar) {
        configureToolbar(toolbar)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        ftueVariant.onNewIntent(intent)
    }

    override fun initUiAndData() {
        ftueVariant.initUiAndData(isFirstCreation())
    }

    // Hack for AccountCreatedFragment
    fun setIsLoading(isLoading: Boolean) {
        ftueVariant.setIsLoading(isLoading)
    }

    companion object {
        const val EXTRA_CONFIG = "EXTRA_CONFIG"

        fun newIntent(context: Context, loginConfig: LoginConfig?): Intent {
            return Intent(context, FTUEActivity::class.java).apply {
                putExtra(EXTRA_CONFIG, loginConfig)
            }
        }

        fun redirectIntent(context: Context, data: Uri?): Intent {
            return Intent(context, FTUEActivity::class.java).apply {
                setData(data)
            }
        }
    }
}

interface FTUEVariant {
    fun onNewIntent(intent: Intent?)
    fun initUiAndData(isFirstCreation: Boolean)
    fun setIsLoading(isLoading: Boolean)
}
