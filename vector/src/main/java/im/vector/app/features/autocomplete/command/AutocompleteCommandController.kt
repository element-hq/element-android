/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.autocomplete.command

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.autocomplete.AutocompleteClickListener
import im.vector.app.features.command.Command
import javax.inject.Inject

class AutocompleteCommandController @Inject constructor(private val stringProvider: StringProvider) : TypedEpoxyController<List<Command>>() {

    var listener: AutocompleteClickListener<Command>? = null

    override fun buildModels(data: List<Command>?) {
        if (data.isNullOrEmpty()) {
            return
        }
        val host = this
        data.forEach { command ->
            autocompleteCommandItem {
                id(command.command)
                name(command.command)
                parameters(command.parameters)
                description(host.stringProvider.getString(command.description))
                clickListener { host.listener?.onItemClick(command) }
            }
        }
    }
}
