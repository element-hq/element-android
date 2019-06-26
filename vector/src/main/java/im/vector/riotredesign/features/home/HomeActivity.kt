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

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.airbnb.mvrx.viewModel
import im.vector.matrix.android.api.Matrix
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.hideKeyboard
import im.vector.riotredesign.core.extensions.observeEvent
import im.vector.riotredesign.core.extensions.replaceFragment
import im.vector.riotredesign.core.platform.OnBackPressed
import im.vector.riotredesign.core.platform.ToolbarConfigurable
import im.vector.riotredesign.core.platform.VectorBaseActivity
import im.vector.riotredesign.core.pushers.PushersManager
import im.vector.riotredesign.features.crypto.keysrequest.KeyRequestHandler
import im.vector.riotredesign.features.crypto.verification.IncomingVerificationRequestHandler
import im.vector.riotredesign.features.notifications.NotificationDrawerManager
import im.vector.riotredesign.features.rageshake.BugReporter
import im.vector.riotredesign.features.rageshake.VectorUncaughtExceptionHandler
import im.vector.riotredesign.features.workers.signout.SignOutUiWorker
import im.vector.riotredesign.features.workers.signout.SignOutViewModel
import im.vector.riotredesign.push.fcm.FcmHelper
import kotlinx.android.synthetic.main.activity_home.*
import org.koin.android.ext.android.inject
import org.koin.android.scope.ext.android.bindScope
import org.koin.android.scope.ext.android.getOrCreateScope


class HomeActivity : VectorBaseActivity(), ToolbarConfigurable {

    // Supported navigation actions for this Activity
    sealed class Navigation {
        object OpenDrawer : Navigation()
    }

    private val homeActivityViewModel: HomeActivityViewModel by viewModel()
    private lateinit var navigationViewModel: HomeNavigationViewModel
    private val homeNavigator by inject<HomeNavigator>()
    private val pushManager by inject<PushersManager>()

    private val notificationDrawerManager by inject<NotificationDrawerManager>()

    // TODO Move this elsewhere
    private val incomingVerificationRequestHandler by inject<IncomingVerificationRequestHandler>()
    // TODO Move this elsewhere
    private val keyRequestHandler by inject<KeyRequestHandler>()

    private var progress: ProgressDialog? = null

    private val drawerListener = object : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerStateChanged(newState: Int) {
            hideKeyboard()
        }
    }

    override fun getLayoutRes() = R.layout.activity_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindScope(getOrCreateScope(HomeModule.HOME_SCOPE))
        homeNavigator.activity = this
        FcmHelper.ensureFcmTokenIsRetrieved(this, pushManager)

        navigationViewModel = ViewModelProviders.of(this).get(HomeNavigationViewModel::class.java)

        drawerLayout.addDrawerListener(drawerListener)
        if (isFirstCreation()) {
            val homeDrawerFragment = HomeDrawerFragment.newInstance()
            val loadingDetail = LoadingFragment.newInstance()
            replaceFragment(loadingDetail, R.id.homeDetailFragmentContainer)
            replaceFragment(homeDrawerFragment, R.id.homeDrawerFragmentContainer)
        }

        homeActivityViewModel.isLoading.observe(this, Observer<Boolean> {
            // TODO better UI
            if (it) {
                progress?.dismiss()
                progress = ProgressDialog(this)
                progress?.setMessage(getString(R.string.room_recents_create_room))
                progress?.show()
            } else {
                progress?.dismiss()
            }
        })

        navigationViewModel.navigateTo.observeEvent(this) { navigation ->
            when (navigation) {
                is Navigation.OpenDrawer -> drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        incomingVerificationRequestHandler.ensureStarted()
        keyRequestHandler.ensureStarted()

        if (intent.hasExtra(EXTRA_CLEAR_EXISTING_NOTIFICATION)) {
            notificationDrawerManager.clearAllEvents()
            intent.removeExtra(EXTRA_CLEAR_EXISTING_NOTIFICATION)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.hasExtra(EXTRA_CLEAR_EXISTING_NOTIFICATION) == true) {
            notificationDrawerManager.clearAllEvents()
            intent.removeExtra(EXTRA_CLEAR_EXISTING_NOTIFICATION)
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

        //Force remote backup state update to update the banner if needed
        ViewModelProviders.of(this).get(SignOutViewModel::class.java).refreshRemoteStateIfNeeded()
    }

    override fun configure(toolbar: Toolbar) {
        configureToolbar(toolbar, false)
    }

    override fun getMenuRes() = R.menu.home

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sliding_menu_sign_out -> {
                SignOutUiWorker(this, notificationDrawerManager)
                        .perform(Matrix.getInstance().currentSession!!)
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
        // if (fm.backStackEntryCount == 0)
        //     return false

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
        private const val EXTRA_CLEAR_EXISTING_NOTIFICATION = "EXTRA_CLEAR_EXISTING_NOTIFICATION"

        fun newIntent(context: Context, clearNotification: Boolean = false): Intent {
            return Intent(context, HomeActivity::class.java)
                    .apply {
                        putExtra(EXTRA_CLEAR_EXISTING_NOTIFICATION, clearNotification)
                    }
        }
    }

}