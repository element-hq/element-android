package im.vector.matrix.android.internal.sync.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceOneTimeKeysCountSyncResponse(
        var signed_curve25519: Int? = null

)