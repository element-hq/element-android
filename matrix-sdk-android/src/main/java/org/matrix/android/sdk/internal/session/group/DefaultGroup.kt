/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.group

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.group.Group
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith

internal class DefaultGroup(override val groupId: String,
                            private val taskExecutor: TaskExecutor,
                            private val getGroupDataTask: GetGroupDataTask) : Group {

    override fun fetchGroupData(callback: MatrixCallback<Unit>): Cancelable {
        val params = GetGroupDataTask.Params.FetchWithIds(listOf(groupId))
        return getGroupDataTask.configureWith(params) {
            this.callback = callback
        }.executeBy(taskExecutor)
    }
}
