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

package im.vector.app.features.login

import android.content.Context
import android.content.Intent
import com.google.android.material.appbar.MaterialToolbar
import im.vector.app.R
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.ToolbarConfigurable
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityLoginBinding
import im.vector.app.features.pin.UnlockedActivity
import im.vector.app.features.settings.VectorPreferences

open class PromptSimplifiedModeActivity : VectorBaseActivity<ActivityLoginBinding>(), ToolbarConfigurable, UnlockedActivity {

    override fun getBinding() = ActivityLoginBinding.inflate(layoutInflater)

    override fun initUiAndData() {
        addFragment(R.id.loginFragmentContainer, PromptSimplifiedModeFragment::class.java)
    }

    companion object {
        fun showIfRequired(context: Context, vectorPreferences: VectorPreferences) {
            if (vectorPreferences.needsSimplifiedModePrompt()) {
                context.startActivity(newIntent(context))
            }
        }
        fun newIntent(context: Context): Intent {
            return Intent(context, PromptSimplifiedModeActivity::class.java)
        }
    }

    override fun configure(toolbar: MaterialToolbar) {
        configureToolbar(toolbar)
    }

    override fun onBackPressed() {
        // Don't call super - we don't want to quit on back press, user should select a mode
    }
}
