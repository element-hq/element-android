package im.vector.riotredesign.features.home.room.detail.timeline.item

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotredesign.R

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class RedactedMessageItem : AbsMessageItem<RedactedMessageItem.Holder>() {

    @EpoxyAttribute
    override lateinit var informationData: MessageInformationData

    override fun getStubType(): Int = STUB_ID

    class Holder : AbsMessageItem.Holder() {
        override fun getStubId(): Int = STUB_ID
    }

    companion object {
        private val STUB_ID = R.id.messageContentRedactedStub
    }
}