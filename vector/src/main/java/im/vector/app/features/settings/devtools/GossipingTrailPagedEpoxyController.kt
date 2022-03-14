/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.settings.devtools

import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.ui.list.GenericItem_
import im.vector.app.core.utils.createUIHandler
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import me.gujun.android.span.span

import org.matrix.android.sdk.internal.crypto.model.AuditTrail
import org.matrix.android.sdk.internal.crypto.model.ForwardInfo
import org.matrix.android.sdk.internal.crypto.model.TrailType
import org.matrix.android.sdk.internal.crypto.model.WithheldInfo

import javax.inject.Inject

class GossipingTrailPagedEpoxyController @Inject constructor(
        private val vectorDateFormatter: VectorDateFormatter,
        private val colorProvider: ColorProvider
) : PagedListEpoxyController<AuditTrail>(
        // Important it must match the PageList builder notify Looper
        modelBuildingHandler = createUIHandler()
) {

    interface InteractionListener {
        fun didTap(event: AuditTrail)
    }

    var interactionListener: InteractionListener? = null

    override fun buildItemModel(currentPosition: Int, item: AuditTrail?): EpoxyModel<*> {
        val host = this
        val event = item ?: return GenericItem_().apply { id(currentPosition) }
        return GenericItem_().apply {
            id(event.hashCode())
            itemClickAction { host.interactionListener?.didTap(event) }
            title(event.type.name.toEpoxyCharSequence())
            description(
                    span {
                        +host.vectorDateFormatter.format(event.ageLocalTs, DateFormatKind.DEFAULT_DATE_AND_TIME)
                        span("\nfrom: ") {
                            textStyle = "bold"
                        }
                        +"${event.info.userId}|${event.info.deviceId}"
                        span("\nroomId: ") {
                            textStyle = "bold"
                        }
                        +event.info.roomId
                        span("\nsessionId: ") {
                            textStyle = "bold"
                        }
                        +event.info.sessionId
                        apply {
                            when (event.type) {
                                TrailType.OutgoingKeyForward  -> {
                                    val fInfo = event.info as ForwardInfo
                                    span("\nchainIndex: ") {
                                        textStyle = "bold"
                                    }
                                    +"${fInfo.chainIndex}"
                                }
                                TrailType.OutgoingKeyWithheld -> {
                                    val fInfo = event.info as WithheldInfo
                                    span("\ncode: ") {
                                        textStyle = "bold"
                                    }
                                    +"${fInfo.code}"
                                }
                                TrailType.IncomingKeyRequest  -> {
                                    // no additional info
                                }
                            }
                        }
                    }.toEpoxyCharSequence()
            )
        }
    }
}
