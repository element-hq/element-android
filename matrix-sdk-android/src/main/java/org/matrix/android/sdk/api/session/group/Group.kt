/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.session.group

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.util.Cancelable

/**
 * This interface defines methods to interact within a group.
 */
interface Group {
    val groupId: String

    /**
     * This methods allows you to refresh data about this group. It will be reflected on the GroupSummary.
     * The SDK also takes care of refreshing group data every hour.
     * @param callback : the matrix callback to be notified of success or failure
     * @return a Cancelable to be able to cancel requests.
     */
    fun fetchGroupData(callback: MatrixCallback<Unit>): Cancelable
}
