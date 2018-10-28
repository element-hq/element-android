package im.vector.riotredesign.features.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.replaceFragment
import im.vector.riotredesign.core.platform.RiotActivity
import im.vector.riotredesign.features.home.detail.LoadingRoomDetailFragment
import im.vector.riotredesign.features.home.detail.RoomDetailFragment
import im.vector.riotredesign.features.home.list.RoomListFragment
import kotlinx.android.synthetic.main.activity_home.*
import org.koin.standalone.StandAloneContext.loadKoinModules


class HomeActivity : RiotActivity(), HomeNavigator {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        loadKoinModules(listOf(HomeModule(this)))
        if (savedInstanceState == null) {
            val roomListFragment = RoomListFragment.newInstance()
            val loadingDetail = LoadingRoomDetailFragment.newInstance()
            replaceFragment(loadingDetail, R.id.homeDetailFragmentContainer)
            replaceFragment(roomListFragment, R.id.homeDrawerFragmentContainer)
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