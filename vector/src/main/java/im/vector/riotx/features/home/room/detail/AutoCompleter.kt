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

package im.vector.riotx.features.home.room.detail

import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.Spannable
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.otaliastudios.autocomplete.Autocomplete
import com.otaliastudios.autocomplete.AutocompleteCallback
import com.otaliastudios.autocomplete.CharPolicy
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.user.model.User
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.matrix.android.api.util.toRoomAliasMatrixItem
import im.vector.riotx.R
import im.vector.riotx.core.glide.GlideApp
import im.vector.riotx.features.autocomplete.command.AutocompleteCommandPresenter
import im.vector.riotx.features.autocomplete.command.CommandAutocompletePolicy
import im.vector.riotx.features.autocomplete.group.AutocompleteGroupPresenter
import im.vector.riotx.features.autocomplete.room.AutocompleteRoomPresenter
import im.vector.riotx.features.autocomplete.user.AutocompleteUserPresenter
import im.vector.riotx.features.command.Command
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.composer.TextComposerViewState
import im.vector.riotx.features.html.PillImageSpan
import im.vector.riotx.features.themes.ThemeUtils
import javax.inject.Inject

class AutoCompleter @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val commandAutocompletePolicy: CommandAutocompletePolicy,
        private val autocompleteCommandPresenter: AutocompleteCommandPresenter,
        private val autocompleteUserPresenter: AutocompleteUserPresenter,
        private val autocompleteRoomPresenter: AutocompleteRoomPresenter,
        private val autocompleteGroupPresenter: AutocompleteGroupPresenter
) {
    private lateinit var fragment: Fragment

    fun enterSpecialMode() {
        commandAutocompletePolicy.enabled = false
    }

    fun exitSpecialMode() {
        commandAutocompletePolicy.enabled = true
    }

    private val glideRequests by lazy {
        GlideApp.with(fragment)
    }

    fun setup(fragment: Fragment, editText: EditText, listener: AutoCompleterListener) {
        this.fragment = fragment

        val elevation = 6f
        val backgroundDrawable = ColorDrawable(ThemeUtils.getColor(fragment.requireContext(), R.attr.riotx_background))
        Autocomplete.on<Command>(editText)
                .with(commandAutocompletePolicy)
                .with(autocompleteCommandPresenter)
                .with(elevation)
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

        autocompleteRoomPresenter.callback = listener
        Autocomplete.on<RoomSummary>(editText)
                .with(CharPolicy('#', true))
                .with(autocompleteRoomPresenter)
                .with(elevation)
                .with(backgroundDrawable)
                .with(object : AutocompleteCallback<RoomSummary> {
                    override fun onPopupItemClicked(editable: Editable, item: RoomSummary): Boolean {
                        // Detect last '#' and remove it
                        var startIndex = editable.lastIndexOf("#")
                        if (startIndex == -1) {
                            startIndex = 0
                        }

                        // Detect next word separator
                        var endIndex = editable.indexOf(" ", startIndex)
                        if (endIndex == -1) {
                            endIndex = editable.length
                        }

                        // Replace the word by its completion
                        val matrixItem = item.toRoomAliasMatrixItem()
                        val displayName = matrixItem.getBestName()

                        // with a trailing space
                        editable.replace(startIndex, endIndex, "$displayName ")

                        // Add the span
                        val span = PillImageSpan(
                                glideRequests,
                                avatarRenderer,
                                fragment.requireContext(),
                                matrixItem
                        )
                        span.bind(editText)

                        editable.setSpan(span, startIndex, startIndex + displayName.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        return true
                    }

                    override fun onPopupVisibilityChanged(shown: Boolean) {
                    }
                })
                .build()

        autocompleteGroupPresenter.callback = listener
        Autocomplete.on<GroupSummary>(editText)
                .with(CharPolicy('+', true))
                .with(autocompleteGroupPresenter)
                .with(elevation)
                .with(backgroundDrawable)
                .with(object : AutocompleteCallback<GroupSummary> {
                    override fun onPopupItemClicked(editable: Editable, item: GroupSummary): Boolean {
                        // Detect last '+' and remove it
                        var startIndex = editable.lastIndexOf("+")
                        if (startIndex == -1) {
                            startIndex = 0
                        }

                        // Detect next word separator
                        var endIndex = editable.indexOf(" ", startIndex)
                        if (endIndex == -1) {
                            endIndex = editable.length
                        }

                        // Replace the word by its completion
                        val matrixItem = item.toMatrixItem()
                        val displayName = matrixItem.getBestName()

                        // with a trailing space
                        editable.replace(startIndex, endIndex, "$displayName ")

                        // Add the span
                        val span = PillImageSpan(
                                glideRequests,
                                avatarRenderer,
                                fragment.requireContext(),
                                matrixItem
                        )
                        span.bind(editText)

                        editable.setSpan(span, startIndex, startIndex + displayName.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        return true
                    }

                    override fun onPopupVisibilityChanged(shown: Boolean) {
                    }
                })
                .build()

        autocompleteUserPresenter.callback = listener
        Autocomplete.on<User>(editText)
                .with(CharPolicy('@', true))
                .with(autocompleteUserPresenter)
                .with(elevation)
                .with(backgroundDrawable)
                .with(object : AutocompleteCallback<User> {
                    override fun onPopupItemClicked(editable: Editable, item: User): Boolean {
                        // Detect last '@' and remove it
                        var startIndex = editable.lastIndexOf("@")
                        if (startIndex == -1) {
                            startIndex = 0
                        }

                        // Detect next word separator
                        var endIndex = editable.indexOf(" ", startIndex)
                        if (endIndex == -1) {
                            endIndex = editable.length
                        }

                        // Replace the word by its completion
                        val matrixItem = item.toMatrixItem()
                        val displayName = matrixItem.getBestName()

                        // with a trailing space
                        editable.replace(startIndex, endIndex, "$displayName ")

                        // Add the span
                        val span = PillImageSpan(
                                glideRequests,
                                avatarRenderer,
                                fragment.requireContext(),
                                matrixItem
                        )
                        span.bind(editText)

                        editable.setSpan(span, startIndex, startIndex + displayName.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        return true
                    }

                    override fun onPopupVisibilityChanged(shown: Boolean) {
                    }
                })
                .build()
    }

    fun render(state: TextComposerViewState) {
        autocompleteUserPresenter.render(state.asyncUsers)
        autocompleteRoomPresenter.render(state.asyncRooms)
        autocompleteGroupPresenter.render(state.asyncGroups)
    }

    interface AutoCompleterListener :
            AutocompleteUserPresenter.Callback,
            AutocompleteRoomPresenter.Callback,
            AutocompleteGroupPresenter.Callback
}
