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

package im.vector.matrix.android.internal.crypto.secrets

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.securestorage.SharedSecretStorageService

internal class DefaultSharedSecureStorage : SharedSecretStorageService {

    override fun addKey(algorithm: String, opts: Map<String, Any>, keyId: String, callback: MatrixCallback<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasKey(keyId: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeSecret(name: String, secretBase64: String, keys: List<String>?, callback: MatrixCallback<Unit>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSecret(name: String, keyId: String, privateKey: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
