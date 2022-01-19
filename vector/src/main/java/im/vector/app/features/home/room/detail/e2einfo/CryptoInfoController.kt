/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.detail.e2einfo

import android.view.View
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import im.vector.app.R
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.dividerItem
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.profiles.notifications.textHeaderItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.Action
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericItem
import im.vector.app.core.ui.list.genericPillItem
import im.vector.app.core.ui.list.verticalMarginItem
import im.vector.app.core.utils.DimensionConverter
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import me.gujun.android.span.span
import org.matrix.android.sdk.internal.crypto.IncomingRoomKeyRequest
import javax.inject.Inject

class CryptoInfoController @Inject constructor(
        private val stringProvider: StringProvider,
        private val dimensionConverter: DimensionConverter,
        private val errorFormatter: ErrorFormatter,
        private val vectorDateFormatter: VectorDateFormatter
) : EpoxyController() {

    interface Callback {
        fun forceShare(request: IncomingRoomKeyRequest)
        fun reRequestKeysForEvent()
    }

    private var state: CryptoInfoViewState? = null
    var callback: Callback? = null

    fun setData(state: CryptoInfoViewState) {
        this.state = state
        requestModelBuild()
    }

    override fun buildModels() {
        val info = state?.e2eInfo ?: return
        val host = this
        if (info is Incomplete) {
            loadingItem {
                id("loading")
            }
            return
        }
        if (info is Fail) {
            errorWithRetryItem {
                id("Error")
                text(host.errorFormatter.toHumanReadable(info.error))
                listener {
                    // host.callback?.retry()
                }
            }
            return
        }
        val successInfo = info.invoke() ?: return

        genericItem {
            id("alg")
            title(host.stringProvider.getString(R.string.encryption_information_algorithm).toEpoxyCharSequence())
            description(successInfo.algorithm.orEmpty().toEpoxyCharSequence())
        }

        genericItem {
            id("sessionId")
            title(host.stringProvider.getString(R.string.encryption_information_session_id).toEpoxyCharSequence())
            description(successInfo.encryptedEventContent.sessionId.orEmpty().toEpoxyCharSequence())
        }

        genericItem {
            id("messageIndex")
            title(host.stringProvider.getString(R.string.encryption_information_ratchet_index).toEpoxyCharSequence())
            description(successInfo.messageIndex.toString().toEpoxyCharSequence())
        }

        val senderInfo = when {
            successInfo.sentByThisDevice -> {
                host.stringProvider.getString(R.string.encryption_information_sent_by_you_from_device, successInfo.sentByUser.deviceId)
            }
            successInfo.sentByMe         -> {
                host.stringProvider.getString(R.string.encryption_information_sent_by_you_from_other_device, successInfo.sentByUser.deviceId)
            }
            else                         -> {
                host.stringProvider.getString(R.string.encryption_information_sent_by_other_from_device, successInfo.sentByUser.userId, successInfo.sentByUser.deviceId)
            }
        }
        // Sender info
        genericItem {
            id("sender_info")
            title(host.stringProvider.getString(R.string.encryption_information_sender_info).toEpoxyCharSequence())
            description(senderInfo.toEpoxyCharSequence())
        }

        val filteredShareWithInfo = successInfo.sharedWithUsers

        if (successInfo.sentByThisDevice) {
            val keyRequestList = successInfo.incomingRoomKeyRequest

            if (keyRequestList.isNotEmpty()) {
                verticalMarginItem {
                    id("top_space")
                    heightInPx(host.dimensionConverter.dpToPx(16))
                }
                dividerItem {
                    id("topDiv")
                }

                textHeaderItem {
                    id("requestHeaders")
                    textRes(R.string.encryption_information_key_requests)
                }

                genericPillItem {
                    id("shared_expl")
                    tintIcon(true)
                    text(host.stringProvider.getString(R.string.encryption_information_key_requests_helper_text).toEpoxyCharSequence())
                }

                buildRoomKeyRequests(keyRequestList, stringProvider, callback, vectorDateFormatter)
            }

            val byUser = filteredShareWithInfo.groupBy { it.userId }

            verticalMarginItem {
                id("request_space")
                heightInPx(host.dimensionConverter.dpToPx(16))
            }
            dividerItem {
                id("request_div")
            }

            textHeaderItem {
                id("sharedWithHeader")
                textRes(R.string.encryption_information_key_share_info)
            }

            genericPillItem {
                id("shared_expl")
                tintIcon(true)
                text(host.stringProvider.getString(R.string.encryption_information_key_share_desc).toEpoxyCharSequence())
            }

            genericFooterItem {
                id("sharedDetails")
                text(
                        host.stringProvider
                                .getString(R.string.encryption_information_shared_with_users_devices, byUser.size, filteredShareWithInfo.size)
                                .toEpoxyCharSequence()
                )
                centered(false)
            }

            byUser
                    .forEach { (userId, deviceInfos) ->

                        genericItem {
                            id("di_$userId")
                            title(userId.toEpoxyCharSequence())
                            description(
                                    span {
                                        +deviceInfos.map { it.deviceId }.joinToString(", ")
                                    }.toEpoxyCharSequence()
                            )
                        }
                    }
        } else {
            // Show info of the session in store
            val knownIndex = successInfo.locallyKnownIndex
            genericItem {
                id("verified")
                title("Local Storage".toEpoxyCharSequence())
                description(
                        if (knownIndex == null) {
                            "This key is not on this device"
                        } else {
                            "The key is on this device (index=$knownIndex)"
                        }.toEpoxyCharSequence()
                )
                endIconResourceId(
                        if (knownIndex == null) {
                            R.drawable.unit_test_ko
                        } else {
                            R.drawable.unit_test_ok
                        }
                )
                apply {
                    if (knownIndex == null) {
                        buttonAction(
                                Action(
                                        title = "Re-Request",
                                        listener = object : ClickListener {
                                            override fun invoke(v: View) {
                                                 host.callback?.reRequestKeysForEvent()
                                            }
                                        })
                        )
                    }
                }
            }

//            genericPillItem {
//                id("local_info")
//                imageRes(R.drawable.ic_crypto_info)
//                text(
//                        "Se"
//                )
//            }
        }
    }
}
