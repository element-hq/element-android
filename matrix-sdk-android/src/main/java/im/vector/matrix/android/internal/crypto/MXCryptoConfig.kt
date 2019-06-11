/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto

/**
 * Class to define the parameters used to customize or configure the end-to-end crypto.
 */
data class MXCryptoConfig(
        // Tell whether the encryption of the event content is enabled for the invited members.
        // By default, we encrypt messages only for the joined members.
        // The encryption for the invited members will be blocked if the history visibility is "joined".
        var enableEncryptionForInvitedMembers: Boolean = false
)
