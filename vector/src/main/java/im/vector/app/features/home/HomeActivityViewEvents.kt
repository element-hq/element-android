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

package im.vector.app.features.home

import im.vector.app.core.platform.VectorViewEvents
import org.matrix.android.sdk.api.util.MatrixItem

sealed interface HomeActivityViewEvents : VectorViewEvents {
    data class AskPasswordToInitCrossSigning(val userItem: MatrixItem.UserItem?) : HomeActivityViewEvents
    data class OnNewSession(val userItem: MatrixItem.UserItem?, val waitForIncomingRequest: Boolean = true) : HomeActivityViewEvents
    data class OnCrossSignedInvalidated(val userItem: MatrixItem.UserItem) : HomeActivityViewEvents
    object PromptToEnableSessionPush : HomeActivityViewEvents
    object ShowAnalyticsOptIn : HomeActivityViewEvents
    object NotifyUserForThreadsMigration : HomeActivityViewEvents
    data class MigrateThreads(val checkSession: Boolean) : HomeActivityViewEvents
}
