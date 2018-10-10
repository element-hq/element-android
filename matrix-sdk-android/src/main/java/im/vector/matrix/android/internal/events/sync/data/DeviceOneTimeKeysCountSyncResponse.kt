package im.vector.matrix.android.internal.events.sync.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceOneTimeKeysCountSyncResponse(
        @Json(name = "signed_curve25519") val signedCurve25519: Int? = null

)