/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.alias

import android.text.InputType
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.profiles.buildProfileSection
import im.vector.app.core.epoxy.profiles.profileActionItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.discovery.settingsButtonItem
import im.vector.app.features.discovery.settingsContinueCancelItem
import im.vector.app.features.discovery.settingsInfoItem
import im.vector.app.features.form.formEditTextItem
import im.vector.app.features.form.formSwitchItem
import im.vector.app.features.roomdirectory.createroom.RoomAliasErrorFormatter
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.MatrixConstants
import org.matrix.android.sdk.api.session.room.alias.RoomAliasError
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomType
import javax.inject.Inject

class RoomAliasController @Inject constructor(
        private val stringProvider: StringProvider,
        private val errorFormatter: ErrorFormatter,
        private val colorProvider: ColorProvider,
        private val roomAliasErrorFormatter: RoomAliasErrorFormatter
) : TypedEpoxyController<RoomAliasViewState>() {

    interface Callback {
        fun toggleManualPublishForm()
        fun setNewAlias(alias: String)
        fun addAlias()
        fun setRoomDirectoryVisibility(roomDirectoryVisibility: RoomDirectoryVisibility)
        fun toggleLocalAliasForm()
        fun setNewLocalAliasLocalPart(aliasLocalPart: String)
        fun addLocalAlias()
        fun openAliasDetail(alias: String)
        fun retry()
    }

    var callback: Callback? = null

    override fun buildModels(data: RoomAliasViewState?) {
        data ?: return

        // Published alias
        buildPublishInfo(data)
        // Room directory visibility
        if (data.roomSummary.invoke()?.roomType != RoomType.SPACE) {
            buildRoomDirectoryVisibility(data)
        }
        // Local alias
        buildLocalInfo(data)
    }

    private fun buildRoomDirectoryVisibility(data: RoomAliasViewState) {
        val host = this
        when (data.roomDirectoryVisibility) {
            Uninitialized -> Unit
            is Loading -> Unit
            is Success -> {
                formSwitchItem {
                    id("roomVisibility")
                    title(host.stringProvider.getString(CommonStrings.room_alias_publish_to_directory, data.homeServerName))
                    switchChecked(data.roomDirectoryVisibility() == RoomDirectoryVisibility.PUBLIC)
                    listener {
                        if (it) {
                            host.callback?.setRoomDirectoryVisibility(RoomDirectoryVisibility.PUBLIC)
                        } else {
                            host.callback?.setRoomDirectoryVisibility(RoomDirectoryVisibility.PRIVATE)
                        }
                    }
                }
            }
            is Fail -> {
                errorWithRetryItem {
                    id("rd_error")
                    text(
                            host.stringProvider.getString(
                                    CommonStrings.room_alias_publish_to_directory_error,
                                    host.errorFormatter.toHumanReadable(data.roomDirectoryVisibility.error)
                            )
                    )
                    listener { host.callback?.retry() }
                }
            }
        }
    }

    private fun buildPublishInfo(data: RoomAliasViewState) {
        val host = this
        buildProfileSection(
                stringProvider.getString(CommonStrings.room_alias_published_alias_title)
        )
        settingsInfoItem {
            id("publishedInfo")
            helperTextResId(CommonStrings.room_alias_published_alias_subtitle)
        }

        data.canonicalAlias
                ?.takeIf { it.isNotEmpty() }
                ?.let { canonicalAlias ->
                    profileActionItem {
                        id("canonical")
                        title(data.canonicalAlias)
                        subtitle(host.stringProvider.getString(CommonStrings.room_alias_published_alias_main))
                        listener { host.callback?.openAliasDetail(canonicalAlias) }
                    }
                }

        if (data.alternativeAliases.isEmpty()) {
            settingsInfoItem {
                id("otherPublishedEmpty")
                if (data.actionPermissions.canChangeCanonicalAlias) {
                    helperTextResId(CommonStrings.room_alias_address_empty_can_add)
                } else {
                    helperTextResId(CommonStrings.room_alias_address_empty)
                }
            }
        } else {
            settingsInfoItem {
                id("otherPublished")
                helperTextResId(CommonStrings.room_alias_published_other)
            }
            data.alternativeAliases.forEachIndexed { idx, altAlias ->
                profileActionItem {
                    id("alt_$idx")
                    title(altAlias)
                    listener { host.callback?.openAliasDetail(altAlias) }
                }
            }
        }

        if (data.actionPermissions.canChangeCanonicalAlias) {
            buildPublishManuallyForm(data)
        }
    }

    private fun buildPublishManuallyForm(data: RoomAliasViewState) {
        val host = this
        when (data.publishManuallyState) {
            RoomAliasViewState.AddAliasState.Hidden -> Unit
            RoomAliasViewState.AddAliasState.Closed -> {
                settingsButtonItem {
                    id("publishManually")
                    colorProvider(host.colorProvider)
                    buttonTitleId(CommonStrings.room_alias_published_alias_add_manually)
                    buttonClickListener { host.callback?.toggleManualPublishForm() }
                }
            }
            is RoomAliasViewState.AddAliasState.Editing -> {
                formEditTextItem {
                    id("publishManuallyEdit")
                    value(data.publishManuallyState.value)
                    maxLength(MatrixConstants.ALIAS_MAX_LENGTH)
                    hint(host.stringProvider.getString(CommonStrings.room_alias_address_hint))
                    inputType(InputType.TYPE_CLASS_TEXT)
                    onTextChange { text ->
                        host.callback?.setNewAlias(text)
                    }
                }
                settingsContinueCancelItem {
                    id("publishManuallySubmit")
                    continueText(host.stringProvider.getString(CommonStrings.room_alias_published_alias_add_manually_submit))
                    continueOnClick { host.callback?.addAlias() }
                    cancelOnClick { host.callback?.toggleManualPublishForm() }
                }
            }
        }
    }

    private fun buildLocalInfo(data: RoomAliasViewState) {
        val host = this
        buildProfileSection(
                stringProvider.getString(CommonStrings.room_alias_local_address_title)
        )
        settingsInfoItem {
            id("localInfo")
            helperText(host.stringProvider.getString(CommonStrings.room_alias_local_address_subtitle, data.homeServerName))
        }

        when (val localAliases = data.localAliases) {
            Uninitialized,
            is Loading -> {
                loadingItem {
                    id("loadingAliases")
                }
            }
            is Success -> {
                if (localAliases().isEmpty()) {
                    settingsInfoItem {
                        id("locEmpty")
                        helperTextResId(CommonStrings.room_alias_local_address_empty)
                    }
                } else {
                    localAliases().forEachIndexed { idx, localAlias ->
                        profileActionItem {
                            id("loc_$idx")
                            title(localAlias)
                            listener { host.callback?.openAliasDetail(localAlias) }
                        }
                    }
                }
            }
            is Fail -> {
                errorWithRetryItem {
                    id("alt_error")
                    text(host.errorFormatter.toHumanReadable(localAliases.error))
                    listener { host.callback?.retry() }
                }
            }
        }

        // Add local
        buildAddLocalAlias(data)
    }

    private fun buildAddLocalAlias(data: RoomAliasViewState) {
        val host = this
        when (data.newLocalAliasState) {
            RoomAliasViewState.AddAliasState.Hidden -> Unit
            RoomAliasViewState.AddAliasState.Closed -> {
                settingsButtonItem {
                    id("newLocalAliasButton")
                    colorProvider(host.colorProvider)
                    buttonTitleId(CommonStrings.room_alias_local_address_add)
                    buttonClickListener { host.callback?.toggleLocalAliasForm() }
                }
            }
            is RoomAliasViewState.AddAliasState.Editing -> {
                formEditTextItem {
                    id("newLocalAlias")
                    value(data.newLocalAliasState.value)
                    suffixText(":" + data.homeServerName)
                    prefixText("#")
                    maxLength(MatrixConstants.maxAliasLocalPartLength(data.homeServerName))
                    hint(host.stringProvider.getString(CommonStrings.room_alias_address_hint))
                    errorMessage(host.roomAliasErrorFormatter.format((data.newLocalAliasState.asyncRequest as? Fail)?.error as? RoomAliasError))
                    onTextChange { value ->
                        host.callback?.setNewLocalAliasLocalPart(value)
                    }
                }
                settingsContinueCancelItem {
                    id("newLocalAliasSubmit")
                    continueText(host.stringProvider.getString(CommonStrings.action_add))
                    continueOnClick { host.callback?.addLocalAlias() }
                    cancelOnClick { host.callback?.toggleLocalAliasForm() }
                }
            }
        }
    }
}
