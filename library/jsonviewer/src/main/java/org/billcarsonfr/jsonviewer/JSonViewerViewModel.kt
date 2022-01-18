/*
 * Copyright (c) 2022 New Vector Ltd
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
