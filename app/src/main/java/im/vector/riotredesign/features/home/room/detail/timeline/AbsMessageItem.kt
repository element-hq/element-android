package im.vector.riotredesign.features.home.room.detail.timeline

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import im.vector.riotredesign.core.epoxy.KotlinModel
import im.vector.riotredesign.features.home.AvatarRenderer

abstract class AbsMessageItem(private val informationData: MessageInformationData,
                              @LayoutRes layoutRes: Int
) : KotlinModel(layoutRes) {

    protected abstract val avatarImageView: ImageView
    protected abstract val memberNameView: TextView
    protected abstract val timeView: TextView

    override fun bind() {
        if (informationData.showInformation) {
            avatarImageView.visibility = View.VISIBLE
            memberNameView.visibility = View.VISIBLE
            timeView.visibility = View.VISIBLE
            timeView.text = informationData.time
            memberNameView.text = informationData.memberName
            AvatarRenderer.render(informationData.avatarUrl, informationData.memberName?.toString(), avatarImageView)
        } else {
            avatarImageView.visibility = View.GONE
            memberNameView.visibility = View.GONE
            timeView.visibility = View.GONE
        }
    }

}