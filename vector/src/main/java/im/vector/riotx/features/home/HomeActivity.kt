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

package im.vector.riotx.features.home

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.airbnb.mvrx.viewModel
import im.vector.riotx.R
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.hideKeyboard
import im.vector.riotx.core.extensions.observeEvent
import im.vector.riotx.core.extensions.replaceFragment
import im.vector.riotx.core.platform.OnBackPressed
import im.vector.riotx.core.platform.ToolbarConfigurable
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.pushers.PushersManager
import im.vector.riotx.features.disclaimer.showDisclaimerDialog
import im.vector.riotx.features.notifications.NotificationDrawerManager
import im.vector.riotx.features.rageshake.VectorUncaughtExceptionHandler
import im.vector.riotx.features.workers.signout.SignOutViewModel
import im.vector.riotx.push.fcm.FcmHelper
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.merge_overlay_waiting_view.*
import timber.log.Timber
import javax.inject.Inject


class HomeActivity : VectorBaseActivity(), ToolbarConfigurable {

    // Supported navigation actions for this Activity
    sealed class Navigation {
        object OpenDrawer : Navigation()
    }

    private val homeActivityViewModel: HomeActivityViewModel by viewModel()
    private lateinit var navigationViewModel: HomeNavigationViewModel

    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var homeActivityViewModelFactory: HomeActivityViewModel.Factory
    @Inject lateinit var homeNavigator: HomeNavigator
    @Inject lateinit var vectorUncaughtExceptionHandler: VectorUncaughtExceptionHandler
    @Inject lateinit var pushManager: PushersManager
    @Inject lateinit var notificationDrawerManager: NotificationDrawerManager

    private var progress: ProgressDialog? = null

    private val drawerListener = object : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerStateChanged(newState: Int) {
            hideKeyboard()
        }
    }

    override fun getLayoutRes() = R.layout.activity_home

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        if (intent.hasExtra(EXTRA_CLEAR_EXISTING_NOTIFICATION)) {
            notificationDrawerManager.clearAllEvents()
            intent.removeExtra(EXTRA_CLEAR_EXISTING_NOTIFICATION)
        }

        activeSessionHolder.getSafeActiveSession()?.getLiveStatus()?.observe(this, Observer { sprogress ->
            Timber.e("${sprogress?.statusText?.let { getString(it) }} ${sprogress?.percentProgress}")
            if (sprogress == null) {
                waiting_view.isVisible = false
            } else {
                waiting_view.setOnClickListener {
                    //block interactions
                }
                waiting_view_status_horizontal_progress.apply {
                    isIndeterminate = false
                    max = 100
                    progress = sprogress.percentProgress
                    isVisible = true
                }
                waiting_view_status_text.apply {
                    text = sprogress.statusText?.let { getString(it) }
                    isVisible = true
                }
                waiting_view.isVisible = true
            }
        })
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

        if (vectorUncaughtExceptionHandler.didAppCrash(this)) {
            vectorUncaughtExceptionHandler.clearAppCrashStatus(this)

            AlertDialog.Builder(this)
                    .setMessage(R.string.send_bug_report_app_crashed)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _, _ -> bugReporter.openBugReportScreen(this) }
                    .setNegativeButton(R.string.no) { _, _ -> bugReporter.deleteCrashFile(this) }
                    .show()
        } else {
            showDisclaimerDialog(this)
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
            R.id.menu_home_suggestion -> {
                bugReporter.openBugReportScreen(this, true)
                return true
            }
            R.id.menu_home_report_bug -> {
                bugReporter.openBugReportScreen(this, false)
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