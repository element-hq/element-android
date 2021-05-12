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
import android.os.Bundle
import androidx.appcompat.widget.SearchView
import com.airbnb.mvrx.MvRx
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySearchBinding

class SearchActivity : VectorBaseActivity<ActivitySearchBinding>() {

    private val searchFragment: SearchFragment?
        get() {
            return supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? SearchFragment
        }

    override fun getBinding() = ActivitySearchBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureToolbar(views.searchToolbar)
    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            val fragmentArgs: SearchArgs = intent?.extras?.getParcelable(MvRx.KEY_ARG) ?: return
            addFragment(R.id.searchFragmentContainer, SearchFragment::class.java, fragmentArgs, FRAGMENT_TAG)
        }
        views.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchFragment?.search(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return true
            }
        })
        // Open the keyboard immediately
        views.searchView.requestFocus()
    }

    companion object {
        private const val FRAGMENT_TAG = "SearchFragment"

        fun newIntent(context: Context, args: SearchArgs): Intent {
            return Intent(context, SearchActivity::class.java).apply {
                // If we do that we will have the same room two times on the stack. Let's allow infinite stack for the moment.
                // flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(MvRx.KEY_ARG, args)
            }
        }
    }
}
