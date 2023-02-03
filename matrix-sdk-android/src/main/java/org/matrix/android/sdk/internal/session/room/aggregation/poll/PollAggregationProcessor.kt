/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.aggregation.poll

import io.realm.Realm
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper

internal interface PollAggregationProcessor {
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
