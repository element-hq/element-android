/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.home.room.detail

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.Spannable
import android.widget.EditText
import com.otaliastudios.autocomplete.Autocomplete
import com.otaliastudios.autocomplete.AutocompleteCallback
import com.otaliastudios.autocomplete.CharPolicy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.glide.GlideRequests
import im.vector.app.features.autocomplete.command.AutocompleteCommandPresenter
import im.vector.app.features.autocomplete.command.CommandAutocompletePolicy
import im.vector.app.features.autocomplete.emoji.AutocompleteEmojiPresenter
import im.vector.app.features.autocomplete.group.AutocompleteGroupPresenter
import im.vector.app.features.autocomplete.member.AutocompleteMemberItem
import im.vector.app.features.autocomplete.member.AutocompleteMemberPresenter
import im.vector.app.features.autocomplete.room.AutocompleteRoomPresenter
import im.vector.app.features.command.Command
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.html.PillImageSpan
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.session.group.model.GroupSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toEveryoneInRoomMatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.api.util.toRoomAliasMatrixItem

class AutoCompleter @AssistedInject constructor(
        @Assisted val roomId: String,
        @Assisted val isInThreadTimeline: Boolean,
        private val avatarRenderer: AvatarRenderer,
        private val commandAutocompletePolicy: CommandAutocompletePolicy,
        AutocompleteCommandPresenterFactory: AutocompleteCommandPresenter.Factory,
        private val autocompleteMemberPresenterFactory: AutocompleteMemberPresenter.Factory,
        private val autocompleteRoomPresenter: AutocompleteRoomPresenter,
        private val autocompleteGroupPresenter: AutocompleteGroupPresenter,
        private val autocompleteEmojiPresenter: AutocompleteEmojiPresenter
) {

    private lateinit var autocompleteMemberPresenter: AutocompleteMemberPresenter

    @AssistedFactory
    interface Factory {
        fun create(roomId: String, isInThreadTimeline: Boolean): AutoCompleter
    }

    private val autocompleteCommandPresenter: AutocompleteCommandPresenter by lazy {
        AutocompleteCommandPresenterFactory.create(isInThreadTimeline)
    }

    private var editText: EditText? = null

    fun enterSpecialMode() {
        commandAutocompletePolicy.enabled = false
    }

    fun exitSpecialMode() {
        commandAutocompletePolicy.enabled = true
    }

    private lateinit var glideRequests: GlideRequests

    fun setup(editText: EditText) {
        this.editText = editText
        glideRequests = GlideApp.with(editText)
        val backgroundDrawable = ColorDrawable(ThemeUtils.getColor(editText.context, android.R.attr.colorBackground))
        setupCommands(backgroundDrawable, editText)
        setupMembers(backgroundDrawable, editText)
        setupGroups(backgroundDrawable, editText)
        setupEmojis(backgroundDrawable, editText)
        setupRooms(backgroundDrawable, editText)
    }

    fun clear() {
        this.editText = null
        autocompleteEmojiPresenter.clear()
        autocompleteGroupPresenter.clear()
        autocompleteRoomPresenter.clear()
        autocompleteCommandPresenter.clear()
        autocompleteMemberPresenter.clear()
    }

    private fun setupCommands(backgroundDrawable: Drawable, editText: EditText) {
        Autocomplete.on<Command>(editText)
                .with(commandAutocompletePolicy)
                .with(autocompleteCommandPresenter)
                .with(ELEVATION_DP)
                .with(backgroundDrawable)
                .with(object : AutocompleteCallback<Command> {
                    override fun onPopupItemClicked(editable: Editable, item: Command): Boolean {
                        editable.clear()
                        editable
                                .append(item.command)
                                .append(" ")
                        return true
                    }

                    override fun onPopupVisibilityChanged(shown: Boolean) {
                    }
                })
                .build()
    }

    private fun setupMembers(backgroundDrawable: ColorDrawable, editText: EditText) {
        autocompleteMemberPresenter = autocompleteMemberPresenterFactory.create(roomId)
        Autocomplete.on<AutocompleteMemberItem>(editText)
                .with(CharPolicy(TRIGGER_AUTO_COMPLETE_MEMBERS, true))
                .with(autocompleteMemberPresenter)
                .with(ELEVATION_DP)
                .with(backgroundDrawable)
                .with(object : AutocompleteCallback<AutocompleteMemberItem> {
                    override fun onPopupItemClicked(editable: Editable, item: AutocompleteMemberItem): Boolean {
                        return when (item) {
                            is AutocompleteMemberItem.Header     -> false // do nothing header is not clickable
                            is AutocompleteMemberItem.RoomMember -> {
                                insertMatrixItem(editText, editable, TRIGGER_AUTO_COMPLETE_MEMBERS, item.roomMemberSummary.toMatrixItem())
                                true
                            }
                            is AutocompleteMemberItem.Everyone   -> {
                                insertMatrixItem(editText, editable, TRIGGER_AUTO_COMPLETE_MEMBERS, item.roomSummary.toEveryoneInRoomMatrixItem())
                                true
                            }
                        }
                    }

                    override fun onPopupVisibilityChanged(shown: Boolean) {
                    }
                })
                .build()
    }

    private fun setupRooms(backgroundDrawable: ColorDrawable, editText: EditText) {
        Autocomplete.on<RoomSummary>(editText)
                .with(CharPolicy(TRIGGER_AUTO_COMPLETE_ROOMS, true))
                .with(autocompleteRoomPresenter)
                .with(ELEVATION_DP)
                .with(backgroundDrawable)
                .with(object : AutocompleteCallback<RoomSummary> {
                    override fun onPopupItemClicked(editable: Editable, item: RoomSummary): Boolean {
                        insertMatrixItem(editText, editable, TRIGGER_AUTO_COMPLETE_ROOMS, item.toRoomAliasMatrixItem())
                        return true
                    }

                    override fun onPopupVisibilityChanged(shown: Boolean) {
                    }
                })
                .build()
    }

    private fun setupGroups(backgroundDrawable: ColorDrawable, editText: EditText) {
        Autocomplete.on<GroupSummary>(editText)
                .with(CharPolicy(TRIGGER_AUTO_COMPLETE_GROUPS, true))
                .with(autocompleteGroupPresenter)
                .with(ELEVATION_DP)
                .with(backgroundDrawable)
                .with(object : AutocompleteCallback<GroupSummary> {
                    override fun onPopupItemClicked(editable: Editable, item: GroupSummary): Boolean {
                        insertMatrixItem(editText, editable, TRIGGER_AUTO_COMPLETE_GROUPS, item.toMatrixItem())
                        return true
                    }

                    override fun onPopupVisibilityChanged(shown: Boolean) {
                    }
                })
                .build()
    }

    private fun setupEmojis(backgroundDrawable: Drawable, editText: EditText) {
        Autocomplete.on<String>(editText)
                .with(CharPolicy(TRIGGER_AUTO_COMPLETE_EMOJIS, false))
                .with(autocompleteEmojiPresenter)
                .with(ELEVATION_DP)
                .with(backgroundDrawable)
                .with(object : AutocompleteCallback<String> {
                    override fun onPopupItemClicked(editable: Editable, item: String): Boolean {
                        // Infer that the last ":" before the current cursor position is the original popup trigger
                        var startIndex = editable.subSequence(0, editText.selectionStart).lastIndexOf(":")
                        if (startIndex == -1) {
                            startIndex = 0
                        }

                        // Detect next word separator
                        var endIndex = editable.indexOf(" ", startIndex)
                        if (endIndex == -1) {
                            endIndex = editable.length
                        }

                        // Replace the word by its completion
                        editable.delete(startIndex, endIndex)
                        editable.insert(startIndex, item)
                        return true
                    }

                    override fun onPopupVisibilityChanged(shown: Boolean) {
                    }
                })
                .build()
    }

    private fun insertMatrixItem(editText: EditText, editable: Editable, firstChar: Char, matrixItem: MatrixItem) {
        // Detect last firstChar and remove it
        var startIndex = editable.lastIndexOf(firstChar)
        if (startIndex == -1) {
            startIndex = 0
        }

        // Detect next word separator
        var endIndex = editable.indexOf(" ", startIndex)
        if (endIndex == -1) {
            endIndex = editable.length
        }

        // Replace the word by its completion
        val displayName = matrixItem.getBestName()

        // Adding trailing space " " or ": " if the user started mention someone
        val displayNameSuffix =
                if (matrixItem is MatrixItem.UserItem) {
                    ": "
                } else {
                    " "
                }

        editable.replace(startIndex, endIndex, "$displayName$displayNameSuffix")

        // Add the span
        val span = PillImageSpan(
                glideRequests,
                avatarRenderer,
                editText.context,
                matrixItem
        )
        span.bind(editText)

        editable.setSpan(span, startIndex, startIndex + displayName.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    companion object {
        private const val ELEVATION_DP = 6f
        private const val TRIGGER_AUTO_COMPLETE_MEMBERS = '@'
        private const val TRIGGER_AUTO_COMPLETE_ROOMS = '#'
        private const val TRIGGER_AUTO_COMPLETE_GROUPS = '+'
        private const val TRIGGER_AUTO_COMPLETE_EMOJIS = ':'
    }
}
