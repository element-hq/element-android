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

package im.vector.app.features.notifications

import im.vector.app.core.resources.BuildMeta
import javax.inject.Inject

/**
 * Util class for creating notifications.
 * Note: Cannot inject ColorProvider in the constructor, because it requires an Activity
 */

data class NotificationActionIds @Inject constructor(
        private val buildMeta: BuildMeta,
) {

    val JOIN_ACTION = "${buildMeta.applicationId}.NotificationActions.JOIN_ACTION"
    val REJECT_ACTION = "${buildMeta.applicationId}.NotificationActions.REJECT_ACTION"
    val QUICK_LAUNCH_ACTION = "${buildMeta.applicationId}.NotificationActions.QUICK_LAUNCH_ACTION"
    val MARK_ROOM_READ_ACTION = "${buildMeta.applicationId}.NotificationActions.MARK_ROOM_READ_ACTION"
    val SMART_REPLY_ACTION = "${buildMeta.applicationId}.NotificationActions.SMART_REPLY_ACTION"
    val DISMISS_SUMMARY_ACTION = "${buildMeta.applicationId}.NotificationActions.DISMISS_SUMMARY_ACTION"
    val DISMISS_ROOM_NOTIF_ACTION = "${buildMeta.applicationId}.NotificationActions.DISMISS_ROOM_NOTIF_ACTION"
    val TAP_TO_VIEW_ACTION = "${buildMeta.applicationId}.NotificationActions.TAP_TO_VIEW_ACTION"
    val DIAGNOSTIC_ACTION = "${buildMeta.applicationId}.NotificationActions.DIAGNOSTIC"
    val PUSH_ACTION = "${buildMeta.applicationId}.PUSH"
}
