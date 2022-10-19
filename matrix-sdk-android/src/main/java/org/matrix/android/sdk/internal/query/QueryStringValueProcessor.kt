/*
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

package org.matrix.android.sdk.internal.query

import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.types.RealmObject
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.query.QueryStringValue.ContentQueryStringValue
import org.matrix.android.sdk.internal.util.Normalizer
import javax.inject.Inject

internal class QueryStringValueProcessor @Inject constructor(
        private val normalizer: Normalizer
) {

    fun <T : RealmObject> RealmQuery<T>.process(field: String, queryStringValue: QueryStringValue): RealmQuery<T> {
        return when (queryStringValue) {
            is QueryStringValue.NoCondition -> this
            is QueryStringValue.IsNotNull -> query("$field != nil")
            is QueryStringValue.IsNull -> query("$field == nil")
            is QueryStringValue.IsEmpty -> query("$field == ''")
            is QueryStringValue.IsNotEmpty -> query("$field != ''")
            is ContentQueryStringValue -> when (queryStringValue) {
                is QueryStringValue.Equals -> query("$field ==${queryStringValue.case.toRealmCase()} $0", queryStringValue.toRealmValue())
                is QueryStringValue.Contains -> query("$field CONTAINS${queryStringValue.case.toRealmCase()} $0", queryStringValue.toRealmValue())
                is QueryStringValue.NotContains -> query("NOT $field CONTAINS${queryStringValue.case.toRealmCase()} $0", queryStringValue.toRealmValue())
            }
        }
    }

    private fun ContentQueryStringValue.toRealmValue(): String {
        return when (case) {
            QueryStringValue.Case.NORMALIZED -> normalizer.normalize(string)
            QueryStringValue.Case.SENSITIVE,
            QueryStringValue.Case.INSENSITIVE -> string
        }
    }

    private fun QueryStringValue.Case.toRealmCase(): String {
        return when (this) {
            QueryStringValue.Case.INSENSITIVE -> "[c]"
            QueryStringValue.Case.SENSITIVE,
            QueryStringValue.Case.NORMALIZED -> ""
        }
    }
}
