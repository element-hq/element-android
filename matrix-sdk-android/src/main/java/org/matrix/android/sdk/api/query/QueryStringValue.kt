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

package org.matrix.android.sdk.api.query

/**
 * Only a subset of [QueryStringValue] are applicable to query the `stateKey` of a state event.
 */
sealed interface QueryStateEventValue

/**
 * Basic query language. All these cases are mutually exclusive.
 */
sealed interface QueryStringValue {
    /**
     * No condition, i.e. there will be no test on the tested field.
     */
    object NoCondition : QueryStringValue

    /**
     * The tested field has to be null.
     */
    object IsNull : QueryStringValue

    /**
     * The tested field has to be not null.
     */
    object IsNotNull : QueryStringValue, QueryStateEventValue

    /**
     * The tested field has to be empty.
     */
    object IsEmpty : QueryStringValue, QueryStateEventValue

    /**
     * The tested field has to be not empty.
     */
    object IsNotEmpty : QueryStringValue, QueryStateEventValue

    /**
     * Interface to check String content.
     */
    sealed interface ContentQueryStringValue : QueryStringValue, QueryStateEventValue {
        val string: String
        val case: Case
    }

    /**
     * The tested field must match the [string].
     */
    data class Equals(override val string: String, override val case: Case = Case.SENSITIVE) : ContentQueryStringValue

    /**
     * The tested field must contain the [string].
     */
    data class Contains(override val string: String, override val case: Case = Case.SENSITIVE) : ContentQueryStringValue

    /**
     * The tested field must not contain the [string].
     */
    data class NotContains(override val string: String, override val case: Case = Case.SENSITIVE) : ContentQueryStringValue

    /**
     * Case enum for [ContentQueryStringValue].
     */
    enum class Case {
        /**
         * Match query sensitive to case.
         */
        SENSITIVE,

        /**
         * Match query insensitive to case, this only works for Latin-1 character sets.
         */
        INSENSITIVE,

        /**
         * Match query with input normalized (case insensitive).
         * Works around Realms inability to sort or filter by case for non Latin-1 character sets.
         * Expects the target field to contain normalized data.
         *
         * @see org.matrix.android.sdk.internal.util.Normalizer.normalize
         */
        NORMALIZED
    }
}

internal fun QueryStringValue.isNormalized() = this is QueryStringValue.ContentQueryStringValue && case == QueryStringValue.Case.NORMALIZED
