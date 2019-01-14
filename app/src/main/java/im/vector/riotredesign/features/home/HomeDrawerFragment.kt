package im.vector.riotredesign.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.replaceChildFragment
import im.vector.riotredesign.core.platform.RiotFragment
import im.vector.riotredesign.features.home.group.GroupListFragment
import im.vector.riotredesign.features.home.room.list.RoomListFragment

class HomeDrawerFragment : RiotFragment() {

    companion object {

        fun newInstance(): HomeDrawerFragment {
            return HomeDrawerFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home_drawer, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null) {
            val groupListFragment = GroupListFragment.newInstance()
            replaceChildFragment(groupListFragment, R.id.groupListFragmentContainer)
            val roomListFragment = RoomListFragment.newInstance()
            replaceChildFragment(roomListFragment, R.id.roomListFragmentContainer)
        }
    }

}