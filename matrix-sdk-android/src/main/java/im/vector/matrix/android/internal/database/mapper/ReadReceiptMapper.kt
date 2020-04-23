package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.room.model.ReadReceipt
import im.vector.matrix.android.api.session.user.model.User
import javax.inject.Inject

class ReadReceiptMapper @Inject constructor() {

    fun map(user_id: String,
            display_name: String?,
            avatar_url: String?,
            origin_server_ts: Double
    ): ReadReceipt {
        val user = User(user_id, display_name, avatar_url)
        return ReadReceipt(user, origin_server_ts.toLong())
    }


}
