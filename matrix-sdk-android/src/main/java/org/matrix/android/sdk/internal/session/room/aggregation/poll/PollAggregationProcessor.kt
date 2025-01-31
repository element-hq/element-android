/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.aggregation.poll

import io.realm.Realm
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper

interface PollAggregationProcessor {
    /**
     * Poll start events don't need to be processed by the aggregator.
     * This function will only handle if the poll is edited and will update the poll summary entity.
     * Returns true if the event is aggregated.
     */
    fun handlePollStartEvent(
            realm: Realm,
            event: Event
    ): Boolean

    /**
     * Aggregates poll response event after many conditional checks like if the poll is ended, if the user is changing his/her vote etc.
     * Returns true if the event is aggregated.
     */
    fun handlePollResponseEvent(
            session: Session,
            realm: Realm,
            event: Event
    ): Boolean

    /**
     * Updates poll summary entity and mark it is ended after many conditional checks like if the poll is already ended etc.
     * Returns true if the event is aggregated.
     */
    fun handlePollEndEvent(
            session: Session,
            powerLevelsHelper: PowerLevelsHelper,
            realm: Realm,
            event: Event
    ): Boolean
}
