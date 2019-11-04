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
package im.vector.riotx.features.reactions

import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import im.vector.riotx.core.platform.VectorViewModel

data class EmojiSearchResultViewState(
        val query: String = "",
        val results: List<EmojiDataSource.EmojiItem> = emptyList()
) : MvRxState

class EmojiSearchResultViewModel(val dataSource: EmojiDataSource, initialState: EmojiSearchResultViewState)
    : VectorViewModel<EmojiSearchResultViewState>(initialState) {

    fun updateQuery(queryString: String) {
        setState {
            copy(
                    query = queryString,
                    results = dataSource.rawData?.emojis?.toList()
                            ?.map { it.second }
                            ?.filter {
                                it.name.contains(queryString, true)
                                        || queryString.split("\\s".toRegex()).fold(true, { prev, q ->
                                    prev && (it.keywords?.any { it.contains(q, true) } ?: false)
                                })
                            } ?: emptyList()
            )
        }
    }

    companion object : MvRxViewModelFactory<EmojiSearchResultViewModel, EmojiSearchResultViewState> {

        override fun create(viewModelContext: ViewModelContext, state: EmojiSearchResultViewState): EmojiSearchResultViewModel? {
            // TODO get the data source from activity? share it with other fragment
            return EmojiSearchResultViewModel(EmojiDataSource(viewModelContext.activity), state)
        }
    }
}
