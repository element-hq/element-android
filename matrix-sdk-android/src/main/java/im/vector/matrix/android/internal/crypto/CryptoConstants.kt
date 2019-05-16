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
 * Matrix algorithm value for olm.
 */
const val MXCRYPTO_ALGORITHM_OLM = "m.olm.v1.curve25519-aes-sha2"

/**
 * Matrix algorithm value for megolm.
 */
const val MXCRYPTO_ALGORITHM_MEGOLM = "m.megolm.v1.aes-sha2"

/**
 * Matrix algorithm value for megolm keys backup.
 */
const val MXCRYPTO_ALGORITHM_MEGOLM_BACKUP = "m.megolm_backup.v1.curve25519-aes-sha2"
