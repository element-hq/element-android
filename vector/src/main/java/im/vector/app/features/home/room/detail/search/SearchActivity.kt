/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.search

import android.content.Context
import android.content.Intent
import com.airbnb.mvrx.MvRx
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity

class SearchActivity : VectorBaseActivity() {

    override fun getLayoutRes() = R.layout.activity_simple

    override fun injectWith(injector: ScreenComponent) {
        super.injectWith(injector)
        injector.inject(this)
    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            val fragmentArgs: SearchArgs = intent?.extras?.getParcelable(MvRx.KEY_ARG) ?: return
            addFragment(R.id.simpleFragmentContainer, SearchFragment::class.java, fragmentArgs)
        }
    }

    companion object {
        fun newIntent(context: Context, args: SearchArgs): Intent {
            return Intent(context, SearchActivity::class.java).apply {
                putExtra(MvRx.KEY_ARG, args)
            }
        }
    }
}
