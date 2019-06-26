package im.vector.riotredesign.features.home.room.detail.timeline.item

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotredesign.R
import im.vector.riotredesign.features.home.AvatarRenderer

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class RedactedMessageItem : AbsMessageItem<RedactedMessageItem.Holder>() {

    @EpoxyAttribute
    override lateinit var informationData: MessageInformationData
    @EpoxyAttribute
    override lateinit var avatarRenderer: AvatarRenderer

    override fun getStubType(): Int = STUB_ID

    override fun shouldShowReactionAtBottom() = false

    class Holder : AbsMessageItem.Holder() {
        override fun getStubId(): Int = STUB_ID
    }

    companion object {
        private val STUB_ID = R.id.messageContentRedactedStub
    }
}