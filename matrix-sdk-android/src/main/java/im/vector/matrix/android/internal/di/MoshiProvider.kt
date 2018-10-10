package im.vector.matrix.android.internal.di

import com.squareup.moshi.Moshi

object MoshiProvider {

    private val moshi: Moshi = Moshi.Builder().build()

    fun providesMoshi(): Moshi {
        return moshi
    }

}