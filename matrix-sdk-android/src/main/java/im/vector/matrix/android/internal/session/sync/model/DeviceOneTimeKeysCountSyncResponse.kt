package im.vector.matrix.android.internal.session.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class DeviceOneTimeKeysCountSyncResponse(
        @Json(name = "signed_curve25519") val signedCurve25519: Int? = null

)