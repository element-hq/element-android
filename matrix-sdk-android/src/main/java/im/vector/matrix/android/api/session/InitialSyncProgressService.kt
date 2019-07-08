package im.vector.matrix.android.api.session

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData

interface InitialSyncProgressService {

    fun getLiveStatus() : LiveData<Status?>

    data class Status(
            @StringRes val statusText: Int?,
            val percentProgress: Int = 0
    )
}