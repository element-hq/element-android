/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
