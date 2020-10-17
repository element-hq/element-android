/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.roomprofile.settings

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.core.epoxy.profiles.buildProfileAction
import im.vector.app.core.epoxy.profiles.buildProfileSection
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.form.formEditTextItem
import im.vector.app.features.home.room.detail.timeline.format.RoomHistoryVisibilityFormatter
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibilityContent
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class RoomSettingsController @Inject constructor(
        private val stringProvider: StringProvider,
        private val roomHistoryVisibilityFormatter: RoomHistoryVisibilityFormatter,
        colorProvider: ColorProvider
) : TypedEpoxyController<RoomSettingsViewState>() {

    interface Callback {
        fun onEnableEncryptionClicked()
        fun onNameChanged(name: String)
        fun onTopicChanged(topic: String)
        fun onHistoryVisibilityClicked()
        fun onAliasChanged(alias: String)
    }

    private val dividerColor = colorProvider.getColorFromAttribute(R.attr.vctr_list_divider_color)

    var callback: Callback? = null

    init {
        setData(null)
    }

    override fun buildModels(data: RoomSettingsViewState?) {
        val roomSummary = data?.roomSummary?.invoke() ?: return

        val historyVisibility = data.historyVisibilityEvent?.let { formatRoomHistoryVisibilityEvent(it) } ?: ""
        val newHistoryVisibility = data.newHistoryVisibility?.let { roomHistoryVisibilityFormatter.format(it) }

        buildProfileSection(
                stringProvider.getString(R.string.settings)
        )

        formEditTextItem {
            id("name")
            enabled(data.actionPermissions.canChangeName)
            value(data.newName ?: roomSummary.displayName)
            hint(stringProvider.getString(R.string.room_settings_name_hint))

            onTextChange { text ->
                callback?.onNameChanged(text)
            }
        }

        formEditTextItem {
            id("topic")
            enabled(data.actionPermissions.canChangeTopic)
            value(data.newTopic ?: roomSummary.topic)
            hint(stringProvider.getString(R.string.room_settings_topic_hint))

            onTextChange { text ->
                callback?.onTopicChanged(text)
            }
        }

        formEditTextItem {
            id("alias")
            enabled(data.actionPermissions.canChangeCanonicalAlias)
            value(data.newCanonicalAlias ?: roomSummary.canonicalAlias)
            hint(stringProvider.getString(R.string.room_settings_addresses_add_new_address))

            onTextChange { text ->
                callback?.onAliasChanged(text)
            }
        }

        buildProfileAction(
                id = "historyReadability",
                title = stringProvider.getString(R.string.room_settings_room_read_history_rules_pref_title),
                subtitle = newHistoryVisibility ?: historyVisibility,
                dividerColor = dividerColor,
                divider = false,
                editable = data.actionPermissions.canChangeHistoryReadability,
                action = { if (data.actionPermissions.canChangeHistoryReadability) callback?.onHistoryVisibilityClicked() }
        )

        buildEncryptionAction(data.actionPermissions, roomSummary)
    }

    private fun buildEncryptionAction(actionPermissions: RoomSettingsViewState.ActionPermissions, roomSummary: RoomSummary) {
        if (!actionPermissions.canEnableEncryption) {
            return
        }
        if (roomSummary.isEncrypted) {
            buildProfileAction(
                    id = "encryption",
                    title = stringProvider.getString(R.string.room_settings_addresses_e2e_enabled),
                    dividerColor = dividerColor,
                    divider = false,
                    editable = false
            )
        } else {
            buildProfileAction(
                    id = "encryption",
                    title = stringProvider.getString(R.string.room_settings_enable_encryption),
                    subtitle = stringProvider.getString(R.string.room_settings_enable_encryption_warning),
                    dividerColor = dividerColor,
                    divider = false,
                    editable = true,
                    action = { callback?.onEnableEncryptionClicked() }
            )
        }
    }

    private fun formatRoomHistoryVisibilityEvent(event: Event): String? {
        val historyVisibility = event.getClearContent().toModel<RoomHistoryVisibilityContent>()?.historyVisibility ?: return null
        return roomHistoryVisibilityFormatter.format(historyVisibility)
    }
}
