/*
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

package org.matrix.android.sdk.internal.session.widgets.helper

import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.session.widgets.model.WidgetContent
import org.matrix.android.sdk.api.session.widgets.model.WidgetType
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.internal.database.RealmSessionProvider
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.displayname.DisplayNameResolver
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberHelper
import org.matrix.android.sdk.internal.session.user.UserDataSource
import java.net.URLEncoder
import javax.inject.Inject

internal class WidgetFactory @Inject constructor(private val userDataSource: UserDataSource,
                                                 private val realmSessionProvider: RealmSessionProvider,
                                                 private val displayNameResolver: DisplayNameResolver,
                                                 private val urlResolver: ContentUrlResolver,
                                                 @UserId private val userId: String) {

    fun create(widgetEvent: Event): Widget? {
        val widgetContent = widgetEvent.content.toModel<WidgetContent>()
        if (widgetContent?.url == null) return null
        val widgetId = widgetEvent.stateKey ?: return null
        val type = widgetContent.type ?: return null
        val senderInfo = if (widgetEvent.senderId == null || widgetEvent.roomId == null) {
            null
        } else {
            realmSessionProvider.withRealm {
                val roomMemberHelper = RoomMemberHelper(it, widgetEvent.roomId)
                val roomMemberSummaryEntity = roomMemberHelper.getLastRoomMember(widgetEvent.senderId)
                SenderInfo(
                        userId = widgetEvent.senderId,
                        displayName = roomMemberSummaryEntity?.displayName,
                        isUniqueDisplayName = roomMemberHelper.isUniqueDisplayName(roomMemberSummaryEntity?.displayName),
                        avatarUrl = roomMemberSummaryEntity?.avatarUrl
                )
            }
        }
        val isAddedByMe = widgetEvent.senderId == userId
        return Widget(
                widgetContent = widgetContent,
                event = widgetEvent,
                widgetId = widgetId,
                senderInfo = senderInfo,
                isAddedByMe = isAddedByMe,
                type = WidgetType.fromString(type)
        )
    }

    // Ref: https://github.com/matrix-org/matrix-widget-api/blob/master/src/templating/url-template.ts#L29-L33
    fun computeURL(widget: Widget, isLightTheme: Boolean): String? {
        var computedUrl = widget.widgetContent.url ?: return null
        val myUser = userDataSource.getUser(userId) ?: User(userId)

        val keyValue = widget.widgetContent.data.mapKeys { "\$${it.key}" }.toMutableMap()

        keyValue[WIDGET_PATTERN_MATRIX_USER_ID] = userId
        keyValue[WIDGET_PATTERN_MATRIX_DISPLAY_NAME] = displayNameResolver.getBestName(myUser.toMatrixItem())
        keyValue[WIDGET_PATTERN_MATRIX_AVATAR_URL] = urlResolver.resolveFullSize(myUser.avatarUrl) ?: ""
        keyValue[WIDGET_PATTERN_MATRIX_WIDGET_ID] = widget.widgetId
        keyValue[WIDGET_PATTERN_MATRIX_ROOM_ID] = widget.event.roomId ?: ""
        keyValue[WIDGET_PATTERN_THEME] = getTheme(isLightTheme)

        for ((key, value) in keyValue) {
            computedUrl = computedUrl.replace(key, URLEncoder.encode(value.toString(), "utf-8"))
        }
        return computedUrl
    }

    private fun getTheme(isLightTheme: Boolean): String {
        return if (isLightTheme) "light" else "dark"
    }

    companion object {
        // Value to be replaced in URLS
        const val WIDGET_PATTERN_MATRIX_USER_ID = "\$matrix_user_id"
        const val WIDGET_PATTERN_MATRIX_DISPLAY_NAME = "\$matrix_display_name"
        const val WIDGET_PATTERN_MATRIX_AVATAR_URL = "\$matrix_avatar_url"
        const val WIDGET_PATTERN_MATRIX_WIDGET_ID = "\$matrix_widget_id"
        const val WIDGET_PATTERN_MATRIX_ROOM_ID = "\$matrix_room_id"
        const val WIDGET_PATTERN_THEME = "\$theme"
    }
}
