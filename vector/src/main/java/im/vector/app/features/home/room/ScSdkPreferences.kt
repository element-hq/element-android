package im.vector.app.features.home.room

import android.content.Context
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class ScSdkPreferences @Inject constructor(private val vectorPreferences: VectorPreferences?): RoomSummary.RoomSummaryPreferenceProvider {

    constructor(context: Context?) : this(vectorPreferences = context?.let { VectorPreferences(it) })

    override fun getUnreadKind(isDirect: Boolean): Int {
        return vectorPreferences?.roomUnreadKind(isDirect) ?: RoomSummary.UNREAD_KIND_FULL
    }

    override fun shouldShowUnimportantCounterBadge(): Boolean {
        return vectorPreferences?.shouldShowUnimportantCounterBadge() ?: false
    }
}
