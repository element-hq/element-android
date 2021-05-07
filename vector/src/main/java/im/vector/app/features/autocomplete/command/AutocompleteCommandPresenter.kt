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

package im.vector.app.features.autocomplete.command

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.features.autocomplete.AutocompleteClickListener
import im.vector.app.features.autocomplete.RecyclerViewPresenter
import im.vector.app.features.command.Command
import im.vector.app.features.settings.VectorPreferences
import javax.inject.Inject

class AutocompleteCommandPresenter @Inject constructor(context: Context,
                                                       private val controller: AutocompleteCommandController,
                                                       private val vectorPreferences: VectorPreferences) :
        RecyclerViewPresenter<Command>(context), AutocompleteClickListener<Command> {

    init {
        controller.listener = this
    }

    override fun instantiateAdapter(): RecyclerView.Adapter<*> {
        return controller.adapter
    }

    override fun onItemClick(t: Command) {
        dispatchClick(t)
    }

    override fun onQuery(query: CharSequence?) {
        val data = Command.values()
                .filter {
                    !it.isDevCommand || vectorPreferences.developerMode()
                }
                .filter {
                    if (query.isNullOrEmpty()) {
                        true
                    } else {
                        it.command.startsWith(query, 1, true)
                    }
                }
        controller.setData(data)
    }

    fun clear() {
        controller.listener = null
    }
}
