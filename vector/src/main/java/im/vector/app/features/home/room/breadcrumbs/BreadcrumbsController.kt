/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.breadcrumbs

import com.airbnb.epoxy.EpoxyController
import im.vector.app.core.epoxy.zeroItem
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class BreadcrumbsController @Inject constructor(
        private val avatarRenderer: AvatarRenderer
) : EpoxyController() {

    var listener: Listener? = null

    private var viewState: BreadcrumbsViewState? = null

    fun update(viewState: BreadcrumbsViewState) {
        this.viewState = viewState
        requestModelBuild()
    }

    override fun buildModels() {
        val safeViewState = viewState ?: return
        val host = this
        // Add a ZeroItem to avoid automatic scroll when the breadcrumbs are updated from another client
        zeroItem {
            id("top")
        }

        // An empty breadcrumbs list can only be temporary because when entering in a room,
        // this one is added to the breadcrumbs
        safeViewState.asyncBreadcrumbs.invoke()
                ?.forEach { roomSummary ->
                    breadcrumbsItem {
                        id(roomSummary.roomId)
                        hasTypingUsers(roomSummary.typingUsers.isNotEmpty())
                        avatarRenderer(host.avatarRenderer)
                        matrixItem(roomSummary.toMatrixItem())
                        unreadNotificationCount(roomSummary.notificationCount)
                        showHighlighted(roomSummary.highlightCount > 0)
                        hasUnreadMessage(roomSummary.hasUnreadMessages)
                        hasDraft(roomSummary.userDrafts.isNotEmpty())
                        itemClickListener {
                            host.listener?.onBreadcrumbClicked(roomSummary.roomId)
                        }
                    }
                }
    }

    interface Listener {
        fun onBreadcrumbClicked(roomId: String)
    }
}
