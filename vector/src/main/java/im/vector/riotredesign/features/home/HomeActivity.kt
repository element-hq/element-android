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

package im.vector.riotredesign.features.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.viewModel
import im.vector.matrix.android.api.Matrix
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.hideKeyboard
import im.vector.riotredesign.core.extensions.observeEvent
import im.vector.riotredesign.core.extensions.replaceFragment
import im.vector.riotredesign.core.platform.OnBackPressed
import im.vector.riotredesign.core.platform.RiotActivity
import im.vector.riotredesign.core.platform.ToolbarConfigurable
import im.vector.riotredesign.features.home.room.detail.LoadingRoomDetailFragment
import im.vector.riotredesign.features.rageshake.BugReporter
import im.vector.riotredesign.features.rageshake.VectorUncaughtExceptionHandler
import im.vector.riotredesign.features.settings.VectorSettingsActivity
import im.vector.riotredesign.features.workers.signout.SignOutUiWorker
import kotlinx.android.synthetic.main.activity_home.*
import org.koin.android.ext.android.inject
import org.koin.android.scope.ext.android.bindScope
import org.koin.android.scope.ext.android.getOrCreateScope


class HomeActivity : RiotActivity(), ToolbarConfigurable {

    private val homeActivityViewModel: HomeActivityViewModel by viewModel()
    private val homeNavigator by inject<HomeNavigator>()

    private val drawerListener = object : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerStateChanged(newState: Int) {
            hideKeyboard()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        bindScope(getOrCreateScope(HomeModule.HOME_SCOPE))
        homeNavigator.activity = this
        drawerLayout.addDrawerListener(drawerListener)
        if (savedInstanceState == null) {
            val homeDrawerFragment = HomeDrawerFragment.newInstance()
            val loadingDetail = LoadingRoomDetailFragment.newInstance()
            replaceFragment(loadingDetail, R.id.homeDetailFragmentContainer)
            replaceFragment(homeDrawerFragment, R.id.homeDrawerFragmentContainer)
        }
        homeActivityViewModel.openRoomLiveData.observeEvent(this) {
            homeNavigator.openRoomDetail(it, null)
        }
    }

    override fun onDestroy() {
        drawerLayout.removeDrawerListener(drawerListener)
        homeNavigator.activity = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        if (VectorUncaughtExceptionHandler.didAppCrash(this)) {
            VectorUncaughtExceptionHandler.clearAppCrashStatus(this)

            AlertDialog.Builder(this)
                    .setMessage(R.string.send_bug_report_app_crashed)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _, _ -> BugReporter.openBugReportScreen(this) }
                    .setNegativeButton(R.string.no) { _, _ -> BugReporter.deleteCrashFile(this) }
                    .show()
        }
    }

    override fun configure(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val drawerToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
    }

    override fun getMenuRes() = R.menu.home

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                return true
            }
            R.id.sliding_menu_settings -> {
                startActivity(VectorSettingsActivity.getIntent(this, "TODO"))
                return true
            }
            R.id.sliding_menu_sign_out -> {
                SignOutUiWorker(this).perform(Matrix.getInstance().currentSession!!)
                return true
            }
        }

        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            val handled = recursivelyDispatchOnBackPressed(supportFragmentManager)
            if (!handled) {
                super.onBackPressed()
            }
        }
    }

    private fun recursivelyDispatchOnBackPressed(fm: FragmentManager): Boolean {
        if (fm.backStackEntryCount == 0)
            return false
        val reverseOrder = fm.fragments.filter { it is OnBackPressed }.reversed()
        for (f in reverseOrder) {
            val handledByChildFragments = recursivelyDispatchOnBackPressed(f.childFragmentManager)
            if (handledByChildFragments) {
                return true
            }
            val backPressable = f as OnBackPressed
            if (backPressable.onBackPressed()) {
                return true
            }
        }
        return false
    }


    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, HomeActivity::class.java)
        }

    }

}