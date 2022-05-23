/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.raw.wellknown

enum class SecureBackupMethod {
    KEY,
    PASSPHRASE,
    KEY_OR_PASSPHRASE;

    val isKeyAvailable: Boolean get() = this == KEY || this == KEY_OR_PASSPHRASE
    val isPassphraseAvailable: Boolean get() = this == PASSPHRASE || this == KEY_OR_PASSPHRASE
}
