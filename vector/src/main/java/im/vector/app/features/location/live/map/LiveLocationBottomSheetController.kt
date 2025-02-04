/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
