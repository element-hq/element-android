package im.vector.matrix.android.internal.sync.data

import com.squareup.moshi.JsonClass

// SyncResponse represents the request response for server sync v2.
@JsonClass(generateAdapter = true)
data class SyncResponse(

        /**
         * The user private data.
         */
        var accountData: Map<String, Any>? = null,

        /**
         * The opaque token for the end.
         */
        var nextBatch: String? = null,

        /**
         * The updates to the presence status of other users.
         */
        var presence: PresenceSyncResponse? = null,

        /*
         * Data directly sent to one of user's devices.
         */
        var toDevice: ToDeviceSyncResponse? = null,

        /**
         * List of rooms.
         */
        var rooms: RoomsSyncResponse? = null,

        /**
         * Devices list update
         */
        var deviceLists: DeviceListResponse? = null,

        /**
         * One time keys management
         */
        var deviceOneTimeKeysCount: DeviceOneTimeKeysCountSyncResponse? = null

)