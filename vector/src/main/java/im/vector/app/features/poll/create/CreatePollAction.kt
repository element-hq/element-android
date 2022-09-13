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

package im.vector.app.features.poll.create

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.model.message.PollType

sealed class CreatePollAction : VectorViewModelAction {
    data class OnQuestionChanged(val question: String) : CreatePollAction()
    data class OnOptionChanged(val index: Int, val option: String) : CreatePollAction()
    data class OnDeleteOption(val index: Int) : CreatePollAction()
    data class OnPollTypeChanged(val pollType: PollType) : CreatePollAction()
    object OnAddOption : CreatePollAction()
    object OnCreatePoll : CreatePollAction()
}
