/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.settings.push

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import org.matrix.android.sdk.api.session.pushers.Pusher

@EpoxyModelClass(layout = R.layout.item_pushgateway)
abstract class PushGatewayItem : EpoxyModelWithHolder<PushGatewayItem.Holder>() {

    @EpoxyAttribute
    lateinit var pusher: Pusher

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.kind.text = when (pusher.kind) {
            // TODO Create const
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
// abstract class ReactionInfoSimpleItem : EpoxyModelWithHolder<ReactionInfoSimpleItem.Holder>() {
