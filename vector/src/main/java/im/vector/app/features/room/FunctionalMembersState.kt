/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.room

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.state.StateService

private const val FUNCTIONAL_MEMBERS_STATE_EVENT_TYPE = "io.element.functional_members"

@JsonClass(generateAdapter = true)
data class FunctionalMembersContent(
        @Json(name = "service_members") val userIds: List<String>? = null
)

fun StateService.getFunctionalMembers(): List<String> {
    return getStateEvent(FUNCTIONAL_MEMBERS_STATE_EVENT_TYPE, QueryStringValue.IsEmpty)
            ?.content
            ?.toModel<FunctionalMembersContent>()
            ?.userIds
            .orEmpty()
}
