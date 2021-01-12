/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.spaces.explore

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem

class SpaceDirectoryController : TypedEpoxyController<SpaceDirectoryState>() {

    override fun buildModels(data: SpaceDirectoryState?) {
        when (data?.summary) {
            is Success -> {
//                val directories = roomDirectoryListCreator.computeDirectories(asyncThirdPartyProtocol())
//
//                directories.forEach {
//                    buildDirectory(it)
//                }
            }
            is Incomplete -> {
                loadingItem {
                    id("loading")
                }
            }
            is Fail -> {
                errorWithRetryItem {
                    id("error")
//                    text(errorFormatter.toHumanReadable(asyncThirdPartyProtocol.error))
//                    listener { callback?.retry() }
                }
            }
            else          -> {
            }
        }
    }
}
