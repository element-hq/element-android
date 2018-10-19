package im.vector.riotredesign.features.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.replaceFragment
import im.vector.riotredesign.core.platform.RiotActivity


class HomeActivity : RiotActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (savedInstanceState == null) {
            val roomListFragment = RoomListFragment.newInstance()
            replaceFragment(roomListFragment, R.id.homeFragmentContainer)
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, HomeActivity::class.java)
        }

    }

}