/*
 * Copyright (c) 2020 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.uploads

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.room.uploads.GetUploadsResult
import org.matrix.android.sdk.api.session.room.uploads.UploadsService
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith

internal class DefaultUploadsService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val taskExecutor: TaskExecutor,
        private val getUploadsTask: GetUploadsTask,
        private val cryptoService: CryptoService
) : UploadsService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): UploadsService
    }

    override fun getUploads(numberOfEvents: Int, since: String?, callback: MatrixCallback<GetUploadsResult>): Cancelable {
        return getUploadsTask
                .configureWith(GetUploadsTask.Params(roomId, cryptoService.isRoomEncrypted(roomId), numberOfEvents, since)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }
}
