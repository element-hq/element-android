/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * licensed under the apache license, version 2.0 (the "license");
 * you may not use this file except in compliance with the license.
 * you may obtain a copy of the license at
 *
 * http://www.apache.org/licenses/license-2.0
 *
 * unless required by applicable law or agreed to in writing, software
 * distributed under the license is distributed on an "as is" basis,
 * without warranties or conditions of any kind, either express or implied.
 * see the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject

internal open class PushConditionEntity(
        var kind: String = "",
        var key: String? = null,
        var pattern: String? = null,
        var iz: String? = null
) : RealmObject() {

    companion object
}
