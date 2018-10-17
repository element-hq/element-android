package im.vector.riotredesign.features.home

import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.room.Room
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotActivity
import org.koin.android.ext.android.inject
import timber.log.Timber


class HomeActivity : RiotActivity() {

    private val matrix by inject<Matrix>()
    private val currentSession = matrix.currentSession!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        currentSession.rooms().observe(this, Observer<List<Room>> { roomList ->
            if (roomList == null) {
                return@Observer
            }
            Timber.v("Observe rooms: %d", roomList.size)
        })
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, HomeActivity::class.java)
        }

    }

}