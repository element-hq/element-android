package im.vector.matrix.android.internal.di

import com.squareup.moshi.Moshi
import im.vector.matrix.android.internal.network.parsing.RuntimeJsonAdapterFactory
import im.vector.matrix.android.internal.network.parsing.UriMoshiAdapter
import im.vector.matrix.android.internal.session.sync.model.UserAccountData
import im.vector.matrix.android.internal.session.sync.model.UserAccountDataDirectMessages
import im.vector.matrix.android.internal.session.sync.model.UserAccountDataFallback

object MoshiProvider {

    private val moshi: Moshi = Moshi.Builder()
            .add(UriMoshiAdapter())
            .add(RuntimeJsonAdapterFactory.of(UserAccountData::class.java, "type", UserAccountDataFallback::class.java)
                         .registerSubtype(UserAccountDataDirectMessages::class.java, UserAccountData.TYPE_DIRECT_MESSAGES)
            )
            .build()

    fun providesMoshi(): Moshi {
        return moshi
    }

}
