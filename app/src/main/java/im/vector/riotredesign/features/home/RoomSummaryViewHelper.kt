package im.vector.riotredesign.features.home

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import com.amulyakhare.textdrawable.TextDrawable
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.firstCharAsString

class RoomSummaryViewHelper(private val roomSummary: RoomSummary) {

    fun avatarDrawable(context: Context): Drawable {
        val avatarColor = ContextCompat.getColor(context, R.color.pale_teal)
        return TextDrawable.builder().buildRound(roomSummary.displayName.firstCharAsString().toUpperCase(), avatarColor)
    }


}