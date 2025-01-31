/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.pushrules

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.internal.util.caseInsensitiveFind

class ContainsDisplayNameCondition : Condition {

    override fun isSatisfied(event: Event, conditionResolver: ConditionResolver): Boolean {
        return conditionResolver.resolveContainsDisplayNameCondition(event, this)
    }

    override fun technicalDescription() = "User is mentioned"

    fun isSatisfied(event: Event, displayName: String): Boolean {
        val message = when (event.type) {
            EventType.MESSAGE -> {
                event.content.toModel<MessageContent>()
            }
            // TODO the spec says:
            // Matches any message whose content is unencrypted and contains the user's current display name
            // EventType.ENCRYPTED -> {
            //     event.root.getClearContent()?.toModel<MessageContent>()
            // }
            else -> null
        } ?: return false

        return message.body.caseInsensitiveFind(displayName)
    }
}
