package im.vector.riotredesign.features.home.group

import android.widget.ImageView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel
import im.vector.riotredesign.core.platform.CheckableFrameLayout
import im.vector.riotredesign.features.home.AvatarRenderer


data class GroupSummaryItem(
        val groupName: CharSequence,
        val avatarUrl: String?,
        val isSelected: Boolean,
        val listener: (() -> Unit)? = null
) : KotlinModel(R.layout.item_group) {

    private val avatarImageView by bind<ImageView>(R.id.groupAvatarImageView)
    private val rootView by bind<CheckableFrameLayout>(R.id.itemGroupLayout)

    override fun bind() {
        rootView.isSelected = isSelected
        rootView.setOnClickListener { listener?.invoke() }
        AvatarRenderer.render(avatarUrl, groupName.toString(), avatarImageView)
    }
}