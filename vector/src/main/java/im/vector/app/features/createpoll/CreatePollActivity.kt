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

package im.vector.app.features.createpoll

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.airbnb.mvrx.viewModel
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import javax.inject.Inject

class CreatePollActivity : SimpleFragmentActivity(), CreatePollViewModel.Factory {

    private val viewModel: CreatePollViewModel by viewModel()
    @Inject lateinit var viewModelFactory: CreatePollViewModel.Factory

    override fun injectWith(injector: ScreenComponent) {
        super.injectWith(injector)
        injector.inject(this)
    }

    override fun create(initialState: CreatePollViewState) = viewModelFactory.create(initialState)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views.toolbar.visibility = View.GONE

        if (isFirstCreation()) {
            addFragment(
                    R.id.container,
                    CreatePollFragment::class.java
            )
        }
    }

    companion object {

        fun getIntent(context: Context): Intent {
            return Intent(context, CreatePollActivity::class.java)
        }
    }
}
