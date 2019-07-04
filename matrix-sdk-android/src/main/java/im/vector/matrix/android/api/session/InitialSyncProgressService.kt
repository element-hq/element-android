package im.vector.matrix.android.api.session

import androidx.lifecycle.LiveData

interface InitialSyncProgressService {

    fun getLiveStatus() : LiveData<Status?>

    data class Status(
            val statusText: Int?,
            val percentProgress: Int = 0
    )
}