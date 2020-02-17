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

package im.vector.matrix.android.internal.crypto.tools

import org.matrix.olm.OlmPkDecryption
import org.matrix.olm.OlmPkEncryption

fun <T> withOlmEncryption(block: (OlmPkEncryption) -> T): T {
    val olmPkEncryption = OlmPkEncryption()
    try {
        return block(olmPkEncryption)
    } finally {
        olmPkEncryption.releaseEncryption()
    }
}

fun <T> withOlmDecryption(block: (OlmPkDecryption) -> T): T {
    val olmPkDecryption = OlmPkDecryption()
    try {
        return block(olmPkDecryption)
    } finally {
        olmPkDecryption.releaseDecryption()
    }
}
