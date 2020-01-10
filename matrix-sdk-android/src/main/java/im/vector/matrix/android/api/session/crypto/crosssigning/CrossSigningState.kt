/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.matrix.android.api.session.crypto.crosssigning

/**
 * Defines the account cross signing state.
 *
 */
enum class CrossSigningState {
    /** Current state is unknown, need to download user keys from server to resolve */
    Unknown,
    /** Currently dowloading user keys*/
    CheckingState,
    /** No Cross signing keys are defined on the server */
    Disabled,
    /** CrossSigning keys are beeing created and uploaded to the server */
    Enabling,
    /** Cross signing keys exists and are trusted*/
    Trusted,
    /** Cross signing keys exists but are not yet trusted*/
    Untrusted,
    /** The local cross signing keys do not match with the server keys*/
    Conflicted
}
