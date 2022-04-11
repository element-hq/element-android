/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.crypto

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

/**
 * Secured Shared Storage algorithm constant
 */
const val SSSS_ALGORITHM_CURVE25519_AES_SHA2 = "m.secret_storage.v1.curve25519-aes-sha2"

/* Secrets are encrypted using AES-CTR-256 and MACed using HMAC-SHA-256. **/
const val SSSS_ALGORITHM_AES_HMAC_SHA2 = "m.secret_storage.v1.aes-hmac-sha2"

// TODO Refacto: use this constants everywhere
const val ed25519 = "ed25519"
const val curve25519 = "curve25519"
