package im.vector.riotredesign.core.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import com.amulyakhare.textdrawable.TextDrawable
import im.vector.riotredesign.R


fun Context.avatarDrawable(name: String): Drawable {
    val avatarColor = ContextCompat.getColor(this, R.color.pale_teal)
    return TextDrawable.builder().buildRound(name.firstCharAsString().toUpperCase(), avatarColor)
}