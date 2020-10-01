/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.crypto.recover

enum class SetupMode {

    /**
     * Only setup cross signing, no 4S or megolm backup
     */
    CROSS_SIGNING_ONLY,

    /**
     * Normal setup mode.
     */
    NORMAL,

    /**
     * Only reset the 4S passphrase/key, but do not touch
     * to existing cross-signing or megolm backup
     * It take the local known secrets and put them in 4S
     */
    PASSPHRASE_RESET,

    /**
     * Resets the passphrase/key, and all missing secrets
     * are re-created. Meaning that if cross signing is setup and the secrets
     * keys are not known, cross signing will be reset (if secret is known we just keep same cross signing)
     * Same apply to megolm
     */
    PASSPHRASE_AND_NEEDED_SECRETS_RESET,

    /**
     * Resets the passphrase/key, cross signing and megolm backup
     */
    HARD_RESET
}
