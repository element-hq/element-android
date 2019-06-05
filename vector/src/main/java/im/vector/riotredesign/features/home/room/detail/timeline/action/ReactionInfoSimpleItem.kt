package im.vector.riotredesign.features.home.room.detail.timeline.action

import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.VectorEpoxyHolder


@EpoxyModelClass(layout = R.layout.item_simple_reaction_info)
abstract class ReactionInfoSimpleItem : EpoxyModelWithHolder<ReactionInfoSimpleItem.Holder>() {

    @EpoxyAttribute
    lateinit var reactionKey: CharSequence
    @EpoxyAttribute
    lateinit var authorDisplayName: CharSequence
    @EpoxyAttribute
    var timeStamp: CharSequence? = null

    override fun bind(holder: Holder) {
        holder.titleView.text = reactionKey
        holder.displayNameView.text = authorDisplayName
        timeStamp?.let {
            holder.timeStampView.text = it
            holder.timeStampView.isVisible = true
        } ?: run {
            holder.timeStampView.isVisible = false
        }
    }

    class Holder : VectorEpoxyHolder() {
        val titleView by bind<TextView>(R.id.itemSimpleReactionInfoKey)
        val displayNameView by bind<TextView>(R.id.itemSimpleReactionInfoMemberName)
        val timeStampView by bind<TextView>(R.id.itemSimpleReactionInfoTime)
    }

}
