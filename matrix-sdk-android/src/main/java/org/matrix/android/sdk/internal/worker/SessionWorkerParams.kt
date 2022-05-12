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

package org.matrix.android.sdk.internal.worker

/**
 * Note about the Worker usage:
 * The workers we chain, or when using the append strategy, should never return Result.Failure(), else the chain will be broken forever
 */
internal interface SessionWorkerParams {
    val sessionId: String

    /**
     * Null when no error occurs. When chaining Workers, first step is to check that there is no lastFailureMessage from the previous workers
     * If it is the case, the worker should just transmit the error and shouldn't do anything else
     */
    val lastFailureMessage: String?
}
