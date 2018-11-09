package im.vector.matrix.android.internal.session.events.prune

import androidx.work.Data
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.di.MoshiProvider

@JsonClass(generateAdapter = true)
internal data class PruneEventWorkerParams(
        val redactionEvents: List<Event>,
        val updateIndexes: List<Int>,
        val deletionIndexes: List<Int>
) {

    fun toData(): Data {
        val json = adapter.toJson(this)
        return Data.Builder().putString(KEY, json).build()
    }

    companion object {

        private val moshi = MoshiProvider.providesMoshi()
        private val adapter = moshi.adapter(PruneEventWorkerParams::class.java)
        private const val KEY = "PruneEventWorkerParams"

        fun fromData(data: Data): PruneEventWorkerParams? {
            val json = data.getString(KEY)
            return if (json == null) {
                null
            } else {
                adapter.fromJson(json)
            }
        }
    }
}