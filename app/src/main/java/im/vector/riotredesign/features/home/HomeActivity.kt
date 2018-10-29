package im.vector.riotredesign.features.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.MenuItem
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.replaceFragment
import im.vector.riotredesign.core.platform.RiotActivity
import im.vector.riotredesign.core.platform.ToolbarConfigurable
import im.vector.riotredesign.features.home.room.detail.LoadingRoomDetailFragment
import im.vector.riotredesign.features.home.room.detail.RoomDetailFragment
import kotlinx.android.synthetic.main.activity_home.*
import org.koin.standalone.StandAloneContext.loadKoinModules


class HomeActivity : RiotActivity(), HomeNavigator, ToolbarConfigurable {


    override fun onCreate(savedInstanceState: Bundle?) {
        loadKoinModules(listOf(HomeModule(this)))
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
            // Android home
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
            super.onBackPressed()
        }
    }

    override fun openRoomDetail(roomId: String) {
        val roomDetailFragment = RoomDetailFragment.newInstance(roomId)
        replaceFragment(roomDetailFragment, R.id.homeDetailFragmentContainer)
        drawerLayout.closeDrawer(Gravity.LEFT)
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, HomeActivity::class.java)
        }

    }

}