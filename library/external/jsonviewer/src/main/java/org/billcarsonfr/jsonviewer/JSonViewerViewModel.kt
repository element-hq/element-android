/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.billcarsonfr.jsonviewer

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import kotlinx.coroutines.launch

internal data class JSonViewerState(
        val root: Async<JSonViewerObject> = Uninitialized
) : MavericksState

internal class JSonViewerViewModel(initialState: JSonViewerState) :
        MavericksViewModel<JSonViewerState>(initialState) {

    fun setJsonSource(json: String, initialOpenDepth: Int) {
        setState {
            copy(root = Loading())
        }
        viewModelScope.launch {
            try {
                ModelParser.fromJsonString(json, initialOpenDepth).let {
                    setState {
                        copy(
                                root = Success(it)
                        )
                    }
                }
            } catch (error: Throwable) {
                setState {
                    copy(
                            root = Fail(error)
                    )
                }
            }
        }
    }

    companion object : MavericksViewModelFactory<JSonViewerViewModel, JSonViewerState> {

        @JvmStatic
        override fun initialState(viewModelContext: ViewModelContext): JSonViewerState? {
            val arg: JSonViewerFragmentArgs = viewModelContext.args()
            return try {
                JSonViewerState(
                        Success(ModelParser.fromJsonString(arg.jsonString, arg.defaultOpenDepth))
                )
            } catch (failure: Throwable) {
                JSonViewerState(Fail(failure))
            }
        }
    }
}
