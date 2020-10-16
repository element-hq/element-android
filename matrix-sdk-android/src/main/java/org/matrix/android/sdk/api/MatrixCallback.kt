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

package org.matrix.android.sdk.api

/**
 * Generic callback interface for asynchronously.
 * @param <T> the type of data to return on success
 */
interface MatrixCallback<in T> {

    /**
     * On success method, default to no-op
     * @param data the data successfully returned from the async function
     */
    fun onSuccess(data: T) {
        // no-op
    }

    /**
     * On failure method, default to no-op
     * @param failure the failure data returned from the async function
     */
    fun onFailure(failure: Throwable) {
        // no-op
    }
}

/**
 * Basic no op implementation
 */
class NoOpMatrixCallback<T> : MatrixCallback<T>
