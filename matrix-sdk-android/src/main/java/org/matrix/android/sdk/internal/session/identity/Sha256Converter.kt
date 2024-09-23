/*
 * Copyright (c) 2024 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.identity

import org.matrix.android.sdk.api.util.toBase64NoPadding
import java.security.MessageDigest
import javax.inject.Inject

class Sha256Converter @Inject constructor() {
    private val sha256 by lazy { MessageDigest.getInstance("SHA-256") }

    fun convertToSha256(str: String): String {
        return sha256.digest(str.toByteArray()).toBase64NoPadding()
    }
}
