package im.vector.matrix.android.internal.util

import androidx.work.Data
import im.vector.matrix.android.internal.di.MoshiProvider

object WorkerParamsFactory {

    const val KEY = "WORKER_PARAMS_JSON"

    inline fun <reified T> toData(params: T): Data {
        val moshi = MoshiProvider.providesMoshi()
        val adapter = moshi.adapter(T::class.java)
        val json = adapter.toJson(params)
        return Data.Builder().putString(KEY, json).build()
    }

    inline fun <reified T> fromData(data: Data): T? {
        val json = data.getString(KEY)
        return if (json == null) {
            null
        } else {
            val moshi = MoshiProvider.providesMoshi()
            val adapter = moshi.adapter(T::class.java)
            adapter.fromJson(json)
        }
    }
}