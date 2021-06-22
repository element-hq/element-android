/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.upgrade

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import im.vector.app.R
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.bottomsheet.bottomSheetTitleItem
import im.vector.app.core.ui.list.ItemStyle
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericProgressBarItem
import im.vector.app.features.form.formSubmitButtonItem
import im.vector.app.features.form.formSwitchItem
import javax.inject.Inject

class MigrateRoomController @Inject constructor(
        private val stringProvider: StringProvider,
        private val errorFormatter: ErrorFormatter
) : TypedEpoxyController<MigrateRoomViewState>() {

    interface InteractionListener {
        fun onAutoInvite(autoInvite: Boolean)
        fun onAutoUpdateParent(update: Boolean)
        fun onConfirmUpgrade()
    }

    var callback: InteractionListener? = null

    override fun buildModels(data: MigrateRoomViewState?) {
        data ?: return

        val host = this@MigrateRoomController

        bottomSheetTitleItem {
            id("title")
            title(
                    host.stringProvider.getString(if (data.isPublic) R.string.upgrade_public_room else R.string.upgrade_private_room)
            )
        }

        genericFooterItem {
            id("warning_text")
            centered(false)
            style(ItemStyle.NORMAL_TEXT)
            text(host.stringProvider.getString(R.string.upgrade_room_warning))
        }

        genericFooterItem {
            id("from_to_room")
            centered(false)
            style(ItemStyle.NORMAL_TEXT)
            text(host.stringProvider.getString(R.string.upgrade_public_room_from_to, data.currentVersion, data.newVersion))
        }

        if (!data.isPublic && data.otherMemberCount > 0) {
            formSwitchItem {
                id("auto_invite")
                switchChecked(data.shouldIssueInvites)
                title(host.stringProvider.getString(R.string.upgrade_room_auto_invite))
                listener { switch -> host.callback?.onAutoInvite(switch) }
            }
        }

        if (data.knownParents.isNotEmpty()) {
            formSwitchItem {
                id("update_parent")
                switchChecked(data.shouldUpdateKnownParents)
                title(host.stringProvider.getString(R.string.upgrade_room_update_parent))
                listener { switch -> host.callback?.onAutoUpdateParent(switch) }
            }
        }
        when (data.upgradingStatus) {
            is Loading -> {
                genericProgressBarItem {
                    id("upgrade_progress")
                    indeterminate(data.upgradingProgressIndeterminate)
                    progress(data.upgradingProgress)
                    total(data.upgradingProgressTotal)
                }
            }
            is Success -> {
                when (val result = data.upgradingStatus.invoke()) {
                    is UpgradeRoomViewModelTask.Result.Failure -> {
                        val errorText = when (result) {
                            is UpgradeRoomViewModelTask.Result.UnknownRoom  -> {
                                // should not happen
                                host.stringProvider.getString(R.string.unknown_error)
                            }
                            is UpgradeRoomViewModelTask.Result.NotAllowed   -> {
                                host.stringProvider.getString(R.string.upgrade_room_no_power_to_manage)
                            }
                            is UpgradeRoomViewModelTask.Result.ErrorFailure -> {
                                host.errorFormatter.toHumanReadable(result.throwable)
                            }
                            else                                            -> null
                        }
                        errorWithRetryItem {
                            id("error")
                            text(errorText)
                            listener { host.callback?.onConfirmUpgrade() }
                        }
                    }
                    is UpgradeRoomViewModelTask.Result.Success -> {
                        // nop, dismisses
                    }
                }
            }
            else       -> {
                formSubmitButtonItem {
                    id("migrate")
                    buttonTitleId(R.string.upgrade)
                    buttonClickListener { host.callback?.onConfirmUpgrade() }
                }
            }
        }
    }
}
