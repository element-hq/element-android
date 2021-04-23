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
package im.vector.app.features.reactions

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.reactions.data.EmojiDataSource
import im.vector.app.features.reactions.data.EmojiItem

data class EmojiSearchResultViewState(
        val query: String = "",
        val results: List<EmojiItem> = emptyList()
) : MvRxState

class EmojiSearchResultViewModel @AssistedInject constructor(
        @Assisted initialState: EmojiSearchResultViewState,
        private val dataSource: EmojiDataSource)
    : VectorViewModel<EmojiSearchResultViewState, EmojiSearchAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: EmojiSearchResultViewState): EmojiSearchResultViewModel
    }

    companion object : MvRxViewModelFactory<EmojiSearchResultViewModel, EmojiSearchResultViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: EmojiSearchResultViewState): EmojiSearchResultViewModel? {
            val activity: EmojiReactionPickerActivity = (viewModelContext as ActivityViewModelContext).activity()
            return activity.emojiSearchResultViewModelFactory.create(state)
        }
    }

    override fun handle(action: EmojiSearchAction) {
        when (action) {
            is EmojiSearchAction.UpdateQuery -> updateQuery(action)
        }
    }

    private fun updateQuery(action: EmojiSearchAction.UpdateQuery) {
        setState {
            copy(
                    query = action.queryString,
                    results = dataSource.filterWith(action.queryString)
            )
        }
    }
}
