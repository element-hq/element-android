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

package org.matrix.android.sdk.api.session.content

interface ContentUploadStateTracker {

    fun track(key: String, updateListener: UpdateListener)

    fun untrack(key: String, updateListener: UpdateListener)

    fun clear()

    interface UpdateListener {
        fun onUpdate(state: State)
    }

    sealed class State {
        object Idle : State()
        object EncryptingThumbnail : State()
        object CompressingImage : State()
        data class CompressingVideo(val percent: Float) : State()
        data class UploadingThumbnail(val current: Long, val total: Long) : State()
        data class Encrypting(val current: Long, val total: Long) : State()
        data class Uploading(val current: Long, val total: Long) : State()
        object Success : State()
        data class Failure(val throwable: Throwable) : State()
    }
}
