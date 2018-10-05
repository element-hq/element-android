package im.vector.matrix.android.internal.events.sync.data

import com.squareup.moshi.JsonClass

// SyncResponse represents the request response for server sync v2.
@JsonClass(generateAdapter = true)
data class SyncResponse(

        /**
         * The user private data.
         */
        val accountData: Map<String, Any> = emptyMap(),

        /**
         * The opaque token for the end.
         */
        val nextBatch: String? = null,

        /**
         * The updates to the presence status of other users.
         */
        val presence: PresenceSyncResponse? = null,

        /*
         * Data directly sent to one of user's devices.
         */
        val toDevice: ToDeviceSyncResponse? = null,

        /**
         * List of rooms.
         */
        val rooms: RoomsSyncResponse? = null,

        /**
         * Devices list update
         */
        val deviceLists: DeviceListResponse? = null,

        /**
         * One time keys management
         */
        val deviceOneTimeKeysCount: DeviceOneTimeKeysCountSyncResponse? = null

)