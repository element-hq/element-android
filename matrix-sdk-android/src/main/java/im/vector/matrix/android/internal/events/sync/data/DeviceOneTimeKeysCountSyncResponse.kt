package im.vector.matrix.android.internal.events.sync.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceOneTimeKeysCountSyncResponse(
        val signed_curve25519: Int? = null

)