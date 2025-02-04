/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.ignored

import com.airbnb.epoxy.EpoxyController
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class IgnoredUsersController @Inject constructor(
        private val stringProvider: StringProvider,
        private val avatarRenderer: AvatarRenderer
) : EpoxyController() {

    var callback: Callback? = null
    private var viewState: IgnoredUsersViewState? = null

    fun update(viewState: IgnoredUsersViewState) {
        this.viewState = viewState
        requestModelBuild()
    }

    override fun buildModels() {
        val nonNullViewState = viewState ?: return
        buildIgnoredUserModels(nonNullViewState.ignoredUsers)
    }

    private fun buildIgnoredUserModels(users: List<User>) {
        val host = this
        if (users.isEmpty()) {
            noResultItem {
                id("empty")
                text(host.stringProvider.getString(CommonStrings.no_ignored_users))
            }
        } else {
            users.forEach { user ->
                userItem {
                    id(user.userId)
                    avatarRenderer(host.avatarRenderer)
                    matrixItem(user.toMatrixItem())
                    itemClickAction { host.callback?.onUserIdClicked(user.userId) }
                }
            }
        }
    }

    interface Callback {
        fun onUserIdClicked(userId: String)
    }
}
