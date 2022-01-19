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

import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import im.vector.app.R
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.dividerItem
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.profiles.notifications.textHeaderItem
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericItem
import im.vector.app.core.ui.list.verticalMarginItem
import im.vector.app.core.utils.DimensionConverter
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import me.gujun.android.span.span
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.internal.crypto.IncomingRoomKeyRequest
import javax.inject.Inject

class SearchCryptoInfoController @Inject constructor(
        private val stringProvider: StringProvider,
        private val dimensionConverter: DimensionConverter,
        private val vectorDateFormatter: VectorDateFormatter
) : EpoxyController() {

    private var state: CryptoInfoViewState? = null
    var callback: CryptoInfoController.Callback? = null

    fun setData(state: CryptoInfoViewState) {
        this.state = state
        requestModelBuild()
    }

    override fun buildModels() {
        val info = state?.e2eInfo ?: return
        val host = this
        val filter = state?.searchFilter.orEmpty()
        if (info is Incomplete) {
            loadingItem {
                id("loading")
            }
            return
        }
        if (info is Fail) {
            errorWithRetryItem {
                id("Error")
                text("host.s.toHumanReadable(roomListAsync.error)")
                listener {
                    // host.callback?.retry()
                }
            }
            return
        }
        val successInfo = info.invoke() ?: return

        val filteredShareWithInfo = successInfo.sharedWithUsers
                .filter { testDeviceInfoWithFilter(filter, it) }

        if (successInfo.sentByThisDevice) {
            val byUser = filteredShareWithInfo.groupBy { it.userId }

            textHeaderItem {
                id("sharedWithHeader")
                textRes(R.string.encryption_information_key_share_info)
            }

            if (filter.isBlank()) {
                genericFooterItem {
                    id("sharedDetails")
                    text(
                            host.stringProvider
                                    .getString(R.string.encryption_information_shared_with_users_devices, byUser.size, filteredShareWithInfo.size)
                                    .toEpoxyCharSequence()
                    )
                    centered(false)
                }
            }

            if (filter.isNotBlank() && filteredShareWithInfo.isEmpty()) {
                genericFooterItem {
                    id("no_filter_shared")
                    text(host.stringProvider.getString(R.string.no_result_placeholder).toEpoxyCharSequence())
                    centered(false)
                }
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

            val keyRequestList = successInfo.incomingRoomKeyRequest
                    .filter { testRoomKeyRequestWithFilter(filter, it) }

            if (keyRequestList.isNotEmpty()) {
                verticalMarginItem {
                    id("request_space")
                    heightInPx(host.dimensionConverter.dpToPx(16))
                }
                dividerItem {
                    id("request_div")
                }

                textHeaderItem {
                    id("requestHeaders")
                    textRes(R.string.encryption_information_key_requests)
                }

                if (filter.isNotBlank() && keyRequestList.isEmpty()) {
                    genericFooterItem {
                        id("no_filter_request")
                        text(host.stringProvider.getString(R.string.no_result_placeholder).toEpoxyCharSequence())
                        centered(false)
                    }
                }

                buildRoomKeyRequests(keyRequestList, stringProvider, callback, vectorDateFormatter)
            }
        }
    }

    private fun testDeviceInfoWithFilter(filter: String, userDeviceInfo: UserDeviceInfo): Boolean {
        if (filter.isBlank()) {
            // No filter
            return true
        }
        return userDeviceInfo.userId.contains(filter, true) || userDeviceInfo.deviceId.contains(filter, true)
    }

    private fun testRoomKeyRequestWithFilter(filter: String, keyRequest: IncomingRoomKeyRequest): Boolean {
        if (filter.isBlank()) {
            // No filter
            return true
        }
        return keyRequest.requestBody?.sessionId?.contains(filter).orTrue() ||
                keyRequest.userId?.contains(filter).orTrue() ||
                keyRequest.deviceId?.contains(filter).orTrue()
    }
}
