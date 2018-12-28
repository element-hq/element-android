package im.vector.riotredesign.features.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.replaceFragment
import im.vector.riotredesign.core.platform.OnBackPressed
import im.vector.riotredesign.core.platform.RiotActivity
import im.vector.riotredesign.core.platform.ToolbarConfigurable
import im.vector.riotredesign.features.home.room.detail.LoadingRoomDetailFragment
import im.vector.riotredesign.features.home.room.detail.RoomDetailFragment
import kotlinx.android.synthetic.main.activity_home.*
import org.koin.standalone.StandAloneContext.loadKoinModules
import timber.log.Timber


class HomeActivity : RiotActivity(), HomeNavigator, ToolbarConfigurable {

    override fun onCreate(savedInstanceState: Bundle?) {
        loadKoinModules(listOf(HomeModule(this).definition))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        if (savedInstanceState == null) {
            val homeDrawerFragment = HomeDrawerFragment.newInstance()
            val loadingDetail = LoadingRoomDetailFragment.newInstance()
            replaceFragment(loadingDetail, R.id.homeDetailFragmentContainer)
            replaceFragment(homeDrawerFragment, R.id.homeDrawerFragmentContainer)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                return true
            }
        }

        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
            drawerLayout.closeDrawer(Gravity.LEFT)
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

    // HomeNavigator *******************************************************************************

    override fun openRoomDetail(roomId: String, eventId: String?) {
        val roomDetailFragment = RoomDetailFragment.newInstance(roomId)
        if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
            closeDrawerLayout(Gravity.LEFT) { replaceFragment(roomDetailFragment, R.id.homeDetailFragmentContainer) }
        } else {
            replaceFragment(roomDetailFragment, R.id.homeDetailFragmentContainer)
        }
    }

    override fun openGroupDetail(groupId: String) {
        Timber.v("Open group detail $groupId")
    }

    override fun openUserDetail(userId: String) {
        Timber.v("Open user detail $userId")
    }

    private fun closeDrawerLayout(gravity: Int, actionOnClose: () -> Unit) {
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(p0: View) {
                drawerLayout.removeDrawerListener(this)
                actionOnClose()
            }
        })
        drawerLayout.closeDrawer(gravity)
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, HomeActivity::class.java)
        }

    }

}