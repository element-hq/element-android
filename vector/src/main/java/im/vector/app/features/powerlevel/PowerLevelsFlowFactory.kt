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

package im.vector.app.features.powerlevel

import androidx.lifecycle.asFlow
import im.vector.app.core.utils.mapOptional
import im.vector.app.core.utils.unwrap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent

class PowerLevelsFlowFactory(private val room: Room) {

    fun createFlow(): Flow<PowerLevelsContent> {
        return room
                .getStateEventLive(EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.NoCondition)
                .asFlow()
                .flowOn(Dispatchers.Default)
                .mapOptional { it.content.toModel<PowerLevelsContent>() }
                .unwrap()
    }
}
