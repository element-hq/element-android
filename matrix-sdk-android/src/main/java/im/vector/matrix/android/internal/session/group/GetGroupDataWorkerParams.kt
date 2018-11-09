package im.vector.matrix.android.internal.session.group

import androidx.work.Data
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.di.MoshiProvider

@JsonClass(generateAdapter = true)
internal data class GetGroupDataWorkerParams(
        val groupIds: List<String>,
        val updateIndexes: List<Int>,
        val deletionIndexes: List<Int>
) {

    fun toData(): Data {
        val json = adapter.toJson(this)
        return Data.Builder().putString(KEY, json).build()
    }

    companion object {

        private val moshi = MoshiProvider.providesMoshi()
        private val adapter = moshi.adapter(GetGroupDataWorkerParams::class.java)
        private const val KEY = "GetGroupDataWorkerParams"

        fun fromData(data: Data): GetGroupDataWorkerParams? {
            val json = data.getString(KEY)
            return if (json == null) {
                null
            } else {
                adapter.fromJson(json)
            }
        }
    }
}