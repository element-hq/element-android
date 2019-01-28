/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.session.room.read

import im.vector.matrix.android.api.MatrixCallback

interface ReadService {

    fun markLatestAsRead(callback: MatrixCallback<Void>)

    fun markAllAsRead(callback: MatrixCallback<Void>)

    fun setReadReceipt(eventId: String, callback: MatrixCallback<Void>)

    fun setReadMarkers(fullyReadEventId: String, readReceiptEventId: String?, callback: MatrixCallback<Void>)

}