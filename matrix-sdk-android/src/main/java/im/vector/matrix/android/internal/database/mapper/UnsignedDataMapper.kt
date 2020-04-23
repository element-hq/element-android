package im.vector.matrix.android.internal.database.mapper

import com.squareup.moshi.JsonDataException
import im.vector.matrix.android.api.session.events.model.UnsignedData
import im.vector.matrix.android.internal.di.MoshiProvider
import timber.log.Timber

internal object UnsignedDataMapper {

    fun mapFromString(us: String?): UnsignedData? {
        return us?.takeIf { it.isNotBlank() }
                ?.let {
                    try {
                        MoshiProvider.providesMoshi().adapter(UnsignedData::class.java).fromJson(it)
                    } catch (t: JsonDataException) {
                        Timber.e(t, "Failed to parse UnsignedData")
                        null
                    }
                }
    }
}
