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

package im.vector.app.features.location.live.map

import com.airbnb.epoxy.EpoxyController
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.resources.DateProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.resources.toTimestamp
import im.vector.app.features.home.AvatarRenderer
import im.vector.lib.core.utils.timer.Clock
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class LiveLocationBottomSheetController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val vectorDateFormatter: VectorDateFormatter,
        private val stringProvider: StringProvider,
        private val clock: Clock,
) : EpoxyController() {

    interface Callback {
        fun onUserSelected(userId: String)
        fun onStopLocationClicked()
    }

    private var userLocations: List<UserLiveLocationViewState>? = null
    var callback: Callback? = null

    fun setData(userLocations: List<UserLiveLocationViewState>) {
        this.userLocations = userLocations
        requestModelBuild()
    }

    override fun buildModels() {
        val currentUserLocations = userLocations ?: return
        val host = this

        val userItemCallback = object : LiveLocationUserItem.Callback {
            override fun onUserSelected(userId: String) {
                host.callback?.onUserSelected(userId)
            }

            override fun onStopSharingClicked() {
                host.callback?.onStopLocationClicked()
            }
        }

        currentUserLocations.forEach { liveLocationViewState ->
            val remainingTime = getFormattedLocalTimeEndOfLive(liveLocationViewState.endOfLiveTimestampMillis)
            liveLocationUserItem {
                id(liveLocationViewState.matrixItem.id)
                callback(userItemCallback)
                matrixItem(liveLocationViewState.matrixItem)
                stringProvider(host.stringProvider)
                clock(host.clock)
                avatarRenderer(host.avatarRenderer)
                remainingTime(remainingTime)
                locationUpdateTimeMillis(liveLocationViewState.locationTimestampMillis)
                showStopSharingButton(liveLocationViewState.showStopSharingButton)
            }
        }
    }

    private fun getFormattedLocalTimeEndOfLive(endOfLiveDateTimestampMillis: Long?): String {
        val endOfLiveDateTime = DateProvider.toLocalDateTime(endOfLiveDateTimestampMillis)
        val formattedDateTime = endOfLiveDateTime.toTimestamp().let { vectorDateFormatter.format(it, DateFormatKind.MESSAGE_SIMPLE) }
        return stringProvider.getString(CommonStrings.location_share_live_until, formattedDateTime)
    }
}
