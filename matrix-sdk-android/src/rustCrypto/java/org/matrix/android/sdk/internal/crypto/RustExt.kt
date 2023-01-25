/*
 * Copyright 2023 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto

import org.matrix.android.sdk.api.session.crypto.model.MessageVerificationState
import org.matrix.rustcomponents.sdk.crypto.VerificationState as InnerVerificationState

fun InnerVerificationState.fromInner(): MessageVerificationState {
    return when (this) {
        InnerVerificationState.VERIFIED -> MessageVerificationState.VERIFIED
        InnerVerificationState.SIGNED_DEVICE_OF_UNVERIFIED_USER -> MessageVerificationState.SIGNED_DEVICE_OF_UNVERIFIED_USER
        InnerVerificationState.UN_SIGNED_DEVICE_OF_VERIFIED_USER -> MessageVerificationState.UN_SIGNED_DEVICE_OF_VERIFIED_USER
        InnerVerificationState.UN_SIGNED_DEVICE_OF_UNVERIFIED_USER -> MessageVerificationState.UN_SIGNED_DEVICE
        InnerVerificationState.UNKNOWN_DEVICE -> MessageVerificationState.UNKNOWN_DEVICE
        InnerVerificationState.UNSAFE_SOURCE -> MessageVerificationState.UNSAFE_SOURCE
    }
}
