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

package im.vector.app.features.autocomplete.emoji

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.features.autocomplete.AutocompleteClickListener
import im.vector.app.features.autocomplete.RecyclerViewPresenter
import im.vector.app.features.reactions.data.EmojiDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import javax.inject.Inject

class AutocompleteEmojiPresenter @Inject constructor(context: Context,
                                                     private val emojiDataSource: EmojiDataSource,
                                                     private val controller: AutocompleteEmojiController) :
        RecyclerViewPresenter<String>(context), AutocompleteClickListener<String> {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        controller.listener = this
    }

    fun clear() {
        coroutineScope.coroutineContext.cancelChildren()
        controller.listener = null
    }

    override fun instantiateAdapter(): RecyclerView.Adapter<*> {
        return controller.adapter
    }

    override fun onItemClick(t: String) {
        dispatchClick(t)
    }

    override fun onQuery(query: CharSequence?) {
        coroutineScope.launch {
            val data = if (query.isNullOrBlank()) {
                // Return common emojis
                emojiDataSource.getQuickReactions()
            } else {
                emojiDataSource.filterWith(query.toString())
            }
            controller.setData(data)
        }
    }
}
