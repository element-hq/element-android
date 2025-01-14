/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.push

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class PushGateWayController @Inject constructor(
        private val stringProvider: StringProvider
) : TypedEpoxyController<PushGatewayViewState>() {

    var interactionListener: PushGatewayItemInteractions? = null

    override fun buildModels(data: PushGatewayViewState?) {
        val host = this
        data?.pushGateways?.invoke()?.let { pushers ->
            if (pushers.isEmpty()) {
                genericFooterItem {
                    id("footer")
                    text(host.stringProvider.getString(CommonStrings.settings_push_gateway_no_pushers).toEpoxyCharSequence())
                }
            } else {
                pushers.forEach {
                    pushGatewayItem {
                        id("${it.pushKey}_${it.appId}")
                        pusher(it)
                        host.interactionListener?.let {
                            interactions(it)
                        }
                    }
                }
            }
        } ?: run {
            genericFooterItem {
                id("loading")
                text(host.stringProvider.getString(CommonStrings.loading).toEpoxyCharSequence())
            }
        }
    }
}
