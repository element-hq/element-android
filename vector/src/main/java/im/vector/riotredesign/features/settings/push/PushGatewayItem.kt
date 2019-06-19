package im.vector.riotredesign.features.settings.push

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.matrix.android.api.session.pushers.Pusher
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.VectorEpoxyHolder


@EpoxyModelClass(layout = R.layout.item_pushgateway)
abstract class PushGatewayItem : EpoxyModelWithHolder<PushGatewayItem.Holder>() {

    @EpoxyAttribute
    lateinit var pusher: Pusher

    override fun bind(holder: Holder) {
        holder.kind.text = when (pusher.kind) {
            "http" -> "Http Pusher"
            "mail" -> "Email Pusher"
            else   -> pusher.kind
        }

        holder.appId.text = pusher.appId
        holder.pushKey.text = pusher.pushKey
        holder.appName.text = pusher.appDisplayName
        holder.url.text = pusher.data.url
        holder.format.text = pusher.data.format
        holder.deviceName.text = pusher.deviceDisplayName
    }


    class Holder : VectorEpoxyHolder() {
        val kind by bind<TextView>(R.id.pushGatewayKind)
        val pushKey by bind<TextView>(R.id.pushGatewayKeyValue)
        val deviceName by bind<TextView>(R.id.pushGatewayDeviceNameValue)
        val format by bind<TextView>(R.id.pushGatewayFormatValue)
        val url by bind<TextView>(R.id.pushGatewayURLValue)
        val appName by bind<TextView>(R.id.pushGatewayAppNameValue)
        val appId by bind<TextView>(R.id.pushGatewayAppIdValue)
    }
}

//
//abstract class ReactionInfoSimpleItem : EpoxyModelWithHolder<ReactionInfoSimpleItem.Holder>() {