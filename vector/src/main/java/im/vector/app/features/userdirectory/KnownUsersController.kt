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

package im.vector.app.features.userdirectory

import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem
import im.vector.app.R
import im.vector.app.core.epoxy.EmptyItem_
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.createUIHandler
import im.vector.app.features.home.AvatarRenderer
import javax.inject.Inject

class KnownUsersController @Inject constructor(private val session: Session,
                                               private val avatarRenderer: AvatarRenderer,
                                               private val stringProvider: StringProvider) : PagedListEpoxyController<User>(
        modelBuildingHandler = createUIHandler()
) {

    private var selectedUsers: List<String> = emptyList()
    private var users: Async<List<User>> = Uninitialized
    private var isFiltering: Boolean = false

    var callback: Callback? = null

    init {
        requestModelBuild()
    }

    fun setData(state: UserDirectoryViewState) {
        this.isFiltering = !state.filterKnownUsersValue.isEmpty()
        val newSelection = state.getSelectedMatrixId()
        this.users = state.knownUsers
        if (newSelection != selectedUsers) {
            this.selectedUsers = newSelection
            requestForcedModelBuild()
        }
        submitList(state.knownUsers())
    }

    override fun buildItemModel(currentPosition: Int, item: User?): EpoxyModel<*> {
        return if (item == null) {
            EmptyItem_().id(currentPosition)
        } else {
            val isSelected = selectedUsers.contains(item.userId)
            UserDirectoryUserItem_()
                    .id(item.userId)
                    .selected(isSelected)
                    .matrixItem(item.toMatrixItem())
                    .avatarRenderer(avatarRenderer)
                    .clickListener { _ ->
                        callback?.onItemClick(item)
                    }
        }
    }

    override fun addModels(models: List<EpoxyModel<*>>) {
        if (users is Incomplete) {
            renderLoading()
        } else if (models.isEmpty()) {
            renderEmptyState()
        } else {
            var lastFirstLetter: String? = null
            for (model in models) {
                if (model is UserDirectoryUserItem) {
                    if (model.matrixItem.id == session.myUserId) continue
                    val currentFirstLetter = model.matrixItem.firstLetterOfDisplayName()
                    val showLetter = !isFiltering && currentFirstLetter.isNotEmpty() && lastFirstLetter != currentFirstLetter
                    lastFirstLetter = currentFirstLetter

                    UserDirectoryLetterHeaderItem_()
                            .id(currentFirstLetter)
                            .letter(currentFirstLetter)
                            .addIf(showLetter, this)

                    model.addTo(this)
                } else {
                    continue
                }
            }
        }
    }

    private fun renderLoading() {
        loadingItem {
            id("loading")
        }
    }

    private fun renderEmptyState() {
        noResultItem {
            id("noResult")
            text(stringProvider.getString(R.string.direct_room_no_known_users))
        }
    }

    interface Callback {
        fun onItemClick(user: User)
    }
}
