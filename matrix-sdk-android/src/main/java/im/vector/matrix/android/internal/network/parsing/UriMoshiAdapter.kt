package im.vector.matrix.android.internal.network.parsing

import android.net.Uri
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

internal class UriMoshiAdapter {

    @ToJson
    fun toJson(uri: Uri): String {
        return uri.toString()
    }

    @FromJson
    fun fromJson(uriString: String): Uri {
        return Uri.parse(uriString)
    }

}