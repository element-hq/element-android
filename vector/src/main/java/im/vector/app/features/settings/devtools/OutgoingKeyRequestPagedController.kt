/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devtools

import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import im.vector.app.core.ui.list.GenericItem_
import im.vector.app.core.utils.createUIHandler
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.crypto.OutgoingKeyRequest
import javax.inject.Inject

class OutgoingKeyRequestPagedController @Inject constructor() : PagedListEpoxyController<OutgoingKeyRequest>(
        // Important it must match the PageList builder notify Looper
        modelBuildingHandler = createUIHandler()
) {

    interface InteractionListener {
        // fun didTap(data: UserAccountData)
    }

    var interactionListener: InteractionListener? = null

    override fun buildItemModel(currentPosition: Int, item: OutgoingKeyRequest?): EpoxyModel<*> {
        val roomKeyRequest = item ?: return GenericItem_().apply { id(currentPosition) }

        return GenericItem_().apply {
            id(roomKeyRequest.requestId)
            title(roomKeyRequest.requestId.toEpoxyCharSequence())
            description(
                    span {
                        span("roomId: ") {
                            textStyle = "bold"
                        }
                        +"${roomKeyRequest.roomId}"

                        span("\nsessionId: ") {
                            textStyle = "bold"
                        }
                        +"${roomKeyRequest.sessionId}"
                        span("\nstate: ") {
                            textStyle = "bold"
                        }
                        +roomKeyRequest.state.name
                    }.toEpoxyCharSequence()
            )
        }
    }
}
