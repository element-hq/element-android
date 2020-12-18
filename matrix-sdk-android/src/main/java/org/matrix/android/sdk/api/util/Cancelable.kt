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

package org.matrix.android.sdk.api.util

/**
 * An interface defining a unique cancel method.
 * It should be used with methods you want to be able to cancel, such as ones interacting with Web Services.
 */
interface Cancelable {

    /**
     * The cancel method, it does nothing by default.
     */
    fun cancel() {
        // no-op
    }
}

object NoOpCancellable : Cancelable
