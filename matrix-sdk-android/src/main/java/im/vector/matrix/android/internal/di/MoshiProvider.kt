package im.vector.matrix.android.internal.di

import com.squareup.moshi.Moshi
import im.vector.matrix.android.internal.network.parsing.UriMoshiAdapter

object MoshiProvider {

    private val moshi: Moshi = Moshi.Builder()
            .add(UriMoshiAdapter())
            .build()

    fun providesMoshi(): Moshi {
        return moshi
    }

}
