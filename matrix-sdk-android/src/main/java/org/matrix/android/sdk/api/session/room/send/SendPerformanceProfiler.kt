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

package org.matrix.android.sdk.api.session.room.send

import org.matrix.android.sdk.api.util.profiling.BaseProfiler

object SendPerformanceProfiler: BaseProfiler<SendPerformanceProfiler.Stages>() {

    enum class Stages() {
        LOCAL_ECHO,
        ENCRYPT_WORKER,
        ENCRYPT_GET_USERS,
        ENCRYPT_SET_ROOM_ENCRYPTION,
        ENCRYPT_MEGOLM_SHARE_KEYS,
        ENCRYPT_MEGOLM_ENCRYPT,
        SEND_WORKER,
        SEND_REQUEST,
        RECEIVED_IN_SYNC
    }

    override val name = "SEND_PROFILER"
}
