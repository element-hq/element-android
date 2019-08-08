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

package im.vector.riotx.features.home.room.detail.readreceipts

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import im.vector.matrix.android.api.session.Session
import im.vector.riotx.R
import im.vector.riotx.core.date.VectorDateFormatter
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.ui.list.genericFooterItem
import im.vector.riotx.core.ui.list.genericLoaderItem
import im.vector.riotx.features.home.AvatarRenderer
import javax.inject.Inject

/**
 * Epoxy controller for read receipt event list
 */
class DisplayReadReceiptsController @Inject constructor(private val dateFormatter: VectorDateFormatter,
                                                        private val stringProvider: StringProvider,
                                                        private val session: Session,
                                                        private val avatarRender: AvatarRenderer)
    : TypedEpoxyController<DisplayReadReceiptsViewState>() {


    override fun buildModels(state: DisplayReadReceiptsViewState) {
        when (state.readReceipts) {
            is Incomplete -> {
                genericLoaderItem {
                    id("loading")
                }
            }
            is Fail       -> {
                genericFooterItem {
                    id("failure")
                    text(stringProvider.getString(R.string.unknown_error))
                }
            }
            is Success    -> {
                state.readReceipts()?.forEach {
                    val timestamp = dateFormatter.formatRelativeDateTime(it.originServerTs)
                    DisplayReadReceiptItem_()
                            .id(it.user.userId)
                            .userId(it.user.userId)
                            .avatarUrl(it.user.avatarUrl)
                            .name(it.user.displayName)
                            .avatarRenderer(avatarRender)
                            .timestamp(timestamp)
                            .addIf(session.myUserId != it.user.userId, this)
                }
            }
        }
    }

}