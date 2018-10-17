package im.vector.riotredesign.features.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotActivity
import kotlinx.android.synthetic.main.activity_home.*
import org.koin.android.ext.android.inject
import timber.log.Timber


class HomeActivity : RiotActivity() {

    private val matrix by inject<Matrix>()
    private val currentSession = matrix.currentSession!!
    private val realmHolder = currentSession.realmHolder()
    private val syncThread = currentSession.syncThread()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val results = realmHolder.instance.where(RoomSummaryEntity::class.java).findAll()
        results.addChangeListener { summaries ->
            Timber.v("Summaries updated")
        }
        startSyncButton.setOnClickListener { syncThread.restart() }
        stopSyncButton.setOnClickListener { syncThread.pause() }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, HomeActivity::class.java)
        }

    }

}