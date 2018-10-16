package im.vector.riotredesign.features.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.events.sync.data.SyncResponse
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotActivity
import kotlinx.android.synthetic.main.activity_home.*
import org.koin.android.ext.android.inject
import timber.log.Timber

class HomeActivity : RiotActivity() {

    private val matrix by inject<Matrix>()
    private val synchronizer = matrix.currentSession?.synchronizer()
    private val realmHolder = matrix.currentSession?.realmHolder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        synchronizeButton.setOnClickListener { synchronize() }
    }

    private fun synchronize() {
        synchronizeButton.visibility = View.GONE
        loadingView.visibility = View.VISIBLE
        synchronizer?.synchronize(object : MatrixCallback<SyncResponse> {
            override fun onSuccess(data: SyncResponse) {
                synchronizeButton.visibility = View.VISIBLE
                loadingView.visibility = View.GONE
                Timber.v("Sync successful")
            }

            override fun onFailure(failure: Failure) {
                synchronizeButton.visibility = View.VISIBLE
                loadingView.visibility = View.GONE
                Timber.e("Sync has failed : %s", failure.toString())
            }
        })
        if (realmHolder != null) {
            val results = realmHolder.instance.where(RoomSummaryEntity::class.java).findAll()
            results.addChangeListener { summaries ->
                Timber.v("Summaries updated")
            }
        }

    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, HomeActivity::class.java)
        }

    }

}