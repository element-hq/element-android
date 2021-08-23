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

package im.vector.app.features.spaces.create

import android.net.Uri
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized

data class CreateSpaceState(
        val name: String? = null,
        val avatarUri: Uri? = null,
        val topic: String = "",
        val step: Step = Step.ChooseType,
        val spaceType: SpaceType? = null,
        val spaceTopology: SpaceTopology? = null,
        val homeServerName: String? = null,
        val aliasLocalPart: String? = null,
        val aliasManuallyModified: Boolean = false,
        val aliasVerificationTask: Async<Boolean> = Uninitialized,
        val nameInlineError: String? = null,
        val defaultRooms: Map<Int /** position in form */, String?>? = null,
        val creationResult: Async<String> = Uninitialized
) : MvRxState {

    enum class Step {
        ChooseType,
        SetDetails,
        AddRooms,
        ChoosePrivateType
    }
}
