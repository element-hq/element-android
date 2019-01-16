package im.vector.riotredesign.features.home

import android.view.Gravity
import androidx.fragment.app.FragmentManager
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.addFragmentToBackstack
import im.vector.riotredesign.core.extensions.replaceFragment
import im.vector.riotredesign.features.home.room.detail.RoomDetailArgs
import im.vector.riotredesign.features.home.room.detail.RoomDetailFragment
import kotlinx.android.synthetic.main.activity_home.*
import timber.log.Timber

class HomeNavigator {

    var activity: HomeActivity? = null

    private var currentRoomId: String? = null

    fun openRoomDetail(roomId: String,
                       eventId: String?,
                       addToBackstack: Boolean = false) {
        Timber.v("Open room detail $roomId - $eventId - $addToBackstack")
        if (!addToBackstack && isRoomOpened(roomId)) {
            return
        }
        activity?.let {
            val args = RoomDetailArgs(roomId, eventId)
            val roomDetailFragment = RoomDetailFragment.newInstance(args)
            it.drawerLayout?.closeDrawer(Gravity.LEFT)
            if (addToBackstack) {
                it.addFragmentToBackstack(roomDetailFragment, R.id.homeDetailFragmentContainer, roomId)
            } else {
                currentRoomId = roomId
                clearBackStack(it.supportFragmentManager)
                it.replaceFragment(roomDetailFragment, R.id.homeDetailFragmentContainer)
            }
        }
    }

    fun openGroupDetail(groupId: String) {
        Timber.v("Open group detail $groupId")
    }

    fun openUserDetail(userId: String) {
        Timber.v("Open user detail $userId")
    }

    fun isRoomOpened(roomId: String): Boolean {
        return currentRoomId == roomId
    }

    private fun clearBackStack(fragmentManager: FragmentManager) {
        if (fragmentManager.backStackEntryCount > 0) {
            val first = fragmentManager.getBackStackEntryAt(0)
            fragmentManager.popBackStack(first.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

}