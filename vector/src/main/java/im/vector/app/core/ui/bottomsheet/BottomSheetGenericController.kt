/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.core.ui.bottomsheet

import com.airbnb.epoxy.TypedEpoxyController

/**
 * Epoxy controller for generic bottom sheet actions.
 */
abstract class BottomSheetGenericController<State : BottomSheetGenericState, Action : BottomSheetGenericRadioAction> :
        TypedEpoxyController<State>() {

    var listener: Listener<Action>? = null

    abstract fun getTitle(): String?

    open fun getSubTitle(): String? = null

    abstract fun getActions(state: State): List<Action>

    override fun buildModels(state: State?) {
        state ?: return
        val host = this
        // Title
        getTitle()?.let { title ->
            bottomSheetTitleItem {
                id("title")
                title(title)
                subTitle(host.getSubTitle())
            }

//            bottomSheetDividerItem {
//                id("title_separator")
//            }
        }
        // Actions
        val actions = getActions(state)
        actions.forEach { action ->
            action.toRadioBottomSheetItem()
                    .listener { listener?.didSelectAction(action) }
                    .addTo(this)
        }
    }

    interface Listener<Action> {
        fun didSelectAction(action: Action)
    }
}
