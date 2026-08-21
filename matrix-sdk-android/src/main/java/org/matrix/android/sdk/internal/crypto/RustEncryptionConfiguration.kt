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

import org.matrix.android.sdk.api.util.toBase64NoPadding
import org.matrix.android.sdk.internal.database.RealmKeysUtils
import org.matrix.android.sdk.internal.di.UserMd5
import org.matrix.android.sdk.internal.session.SessionScope
import java.security.MessageDigest
import javax.inject.Inject

@SessionScope
internal class RustEncryptionConfiguration @Inject constructor(
        @UserMd5 private val userMd5: String,
        private val realmKeyUtil: RealmKeysUtils,
) {

    // New SHA-256 based alias derived from the legacy MD5 hash
    private val userSha256: String by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(userMd5.toByteArray())
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private val legacyAlias = "crypto_module_rust_$userMd5"
    private val newAlias: String get() = "crypto_module_rust_sha256_$userSha256"

    fun getDatabasePassphrase(): String {
        // Migration strategy: if a key exists under the legacy MD5 alias, keep using it
        // for backward compatibility. New installations will use the SHA-256 alias.
        return if (realmKeyUtil.hasRealmEncryptionKey(legacyAlias)) {
            realmKeyUtil.getRealmEncryptionKey(legacyAlias).toBase64NoPadding()
        } else {
            realmKeyUtil.getRealmEncryptionKey(newAlias).toBase64NoPadding()
        }
    }
}
