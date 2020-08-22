package im.vector.app.features.roomprofile.members

import android.content.Context
import android.content.Intent
import com.airbnb.mvrx.MvRx
import im.vector.app.R
import im.vector.app.core.extensions.addFragment
import im.vector.app.features.roomprofile.RoomProfileActivity
import im.vector.app.features.roomprofile.RoomProfileArgs

class RoomMemberListActivity :
        RoomProfileActivity() {

    companion object {

        fun newIntent(context: Context, roomId: String): Intent {
            val roomProfileArgs = RoomProfileArgs(roomId)
            return Intent(context, RoomMemberListActivity::class.java).apply {
                putExtra(MvRx.KEY_ARG, roomProfileArgs)
            }
        }
    }

    override fun addInitialFragment() {
        addFragment(R.id.simpleFragmentContainer, RoomMemberListFragment::class.java, roomProfileArgs)
    }
}
