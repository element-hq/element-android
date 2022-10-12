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

package im.vector.app.core.session.clientinfo

import im.vector.app.core.di.ActiveSessionHolder
import timber.log.Timber
import javax.inject.Inject

/**
 * This use case delete the account data event containing extended client info.
 */
class DeleteMatrixClientInfoUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val setMatrixClientInfoUseCase: SetMatrixClientInfoUseCase,
) {

    suspend fun execute(): Result<Unit> = runCatching {
        Timber.d("deleting recorded client info")
        val session = activeSessionHolder.getActiveSession()
        val emptyClientInfo = MatrixClientInfoContent(
                name = "",
                version = "",
                url = "",
        )
        return setMatrixClientInfoUseCase.execute(session, emptyClientInfo)
    }
}
