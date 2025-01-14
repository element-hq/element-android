/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.reactions

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.reactions.data.EmojiDataSource
import im.vector.app.features.reactions.data.EmojiItem
import kotlinx.coroutines.launch

data class EmojiSearchResultViewState(
        val query: String = "",
        val results: List<EmojiItem> = emptyList()
) : MavericksState

class EmojiSearchResultViewModel @AssistedInject constructor(
        @Assisted initialState: EmojiSearchResultViewState,
        private val dataSource: EmojiDataSource
) :
        VectorViewModel<EmojiSearchResultViewState, EmojiSearchAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<EmojiSearchResultViewModel, EmojiSearchResultViewState> {
        override fun create(initialState: EmojiSearchResultViewState): EmojiSearchResultViewModel
    }

    companion object : MavericksViewModelFactory<EmojiSearchResultViewModel, EmojiSearchResultViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: EmojiSearchAction) {
        when (action) {
            is EmojiSearchAction.UpdateQuery -> updateQuery(action)
        }
    }

    private fun updateQuery(action: EmojiSearchAction.UpdateQuery) {
        viewModelScope.launch {
            val results = dataSource.filterWith(action.queryString)
            setState {
                copy(
                        query = action.queryString,
                        results = results
                )
            }
        }
    }
}
