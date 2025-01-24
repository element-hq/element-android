/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live.map

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import im.vector.app.features.location.toLocationData
import kotlinx.coroutines.suspendCancellableCoroutine
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getUserOrDefault
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject
import kotlin.coroutines.resume

class UserLiveLocationViewStateMapper @Inject constructor(
        private val locationPinProvider: LocationPinProvider,
        private val activeSessionHolder: ActiveSessionHolder,
) {

    suspend fun map(liveLocationShareAggregatedSummary: LiveLocationShareAggregatedSummary) =
            suspendCancellableCoroutine<UserLiveLocationViewState?> { continuation ->
                val userId = liveLocationShareAggregatedSummary.userId
                val locationData = liveLocationShareAggregatedSummary.lastLocationDataContent
                        ?.getBestLocationInfo()
                        ?.geoUri
                        .toLocationData()

                when {
                    userId.isNullOrEmpty() || locationData == null -> continuation.resume(null)
                    else -> {
                        val session = activeSessionHolder.getActiveSession()
                        val roomId = liveLocationShareAggregatedSummary.roomId
                        val matrixItem = if (roomId != null) {
                            session.getRoom(roomId)
                                    ?.membershipService()
                                    ?.getRoomMember(userId)
                                    ?.toMatrixItem()
                                    ?: MatrixItem.UserItem(userId)
                        } else {
                            session.getUserOrDefault(userId).toMatrixItem()
                        }
                        locationPinProvider.create(matrixItem) { pinDrawable ->
                            val locationTimestampMillis = liveLocationShareAggregatedSummary.lastLocationDataContent?.getBestTimestampMillis()
                            val viewState = UserLiveLocationViewState(
                                    matrixItem = matrixItem,
                                    pinDrawable = pinDrawable,
                                    locationData = locationData,
                                    endOfLiveTimestampMillis = liveLocationShareAggregatedSummary.endOfLiveTimestampMillis,
                                    locationTimestampMillis = locationTimestampMillis,
                                    showStopSharingButton = userId == session.myUserId
                            )
                            continuation.resume(viewState)
                        }
                    }
                }
            }
}
