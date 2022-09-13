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

package im.vector.app.features.invite

import javax.inject.Inject

/**
 * This interface defines 2 flags so you can handle auto accept invites.
 * At the moment we only have [CompileTimeAutoAcceptInvites] implementation.
 */
interface AutoAcceptInvites {
    /**
     * Enable auto-accept invites. It means, as soon as you got an invite from the sync, it will try to join it.
     */
    val isEnabled: Boolean

    /**
     * Hide invites from the UI (from notifications, notification count and room list). By default invites are hidden when [isEnabled] is true
     */
    val hideInvites: Boolean
        get() = isEnabled
}

fun AutoAcceptInvites.showInvites() = !hideInvites

/**
 * Simple compile time implementation of AutoAcceptInvites flags.
 */
class CompileTimeAutoAcceptInvites @Inject constructor() : AutoAcceptInvites {
    override val isEnabled = false
}
