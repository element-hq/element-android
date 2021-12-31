/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.usercode

import com.airbnb.mvrx.MavericksState
import org.matrix.android.sdk.api.util.MatrixItem

data class UserCodeState(
        val userId: String,
        val matrixItem: MatrixItem? = null,
        val shareLink: String? = null,
        val mode: Mode = Mode.SHOW
) : MavericksState {
    sealed class Mode {
        object SHOW : Mode()
        object SCAN : Mode()
        data class RESULT(val matrixItem: MatrixItem, val rawLink: String) : Mode()
    }

    constructor(args: UserCodeActivity.Args) : this(
            userId = args.userId
    )
}
