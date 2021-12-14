/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
