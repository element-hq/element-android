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

package fr.gouv.tchap.android.sdk.internal.session.user.accountdata

import fr.gouv.tchap.android.sdk.api.session.accountdata.HideProfileContent
import org.matrix.android.sdk.internal.session.user.accountdata.UpdateUserAccountDataTask
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface UpdateHideProfileTask : Task<UpdateHideProfileTask.Params, Unit> {
    data class Params(
            val hideProfile: Boolean
    )
}

internal class TchapUpdateHideProfileTask @Inject constructor(
        private val updateUserAccountDataTask: UpdateUserAccountDataTask
) : UpdateHideProfileTask {

    override suspend fun execute(params: UpdateHideProfileTask.Params) {
        return updateUserAccountDataTask.execute(UpdateUserAccountDataTask.HideProfile(
                hideProfileContent = HideProfileContent(params.hideProfile)
        ))
    }
}
