/*
 * Copyright (c) 2023 New Vector Ltd
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

package org.matrix.android.sdk.api.auth.certs

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.matrix.android.sdk.api.network.ssl.Fingerprint
import org.matrix.android.sdk.internal.di.MatrixScope
import javax.inject.Inject

/**
 * Object to store and retrieve home and identity server urls.
 */
@MatrixScope
class TrustedCertificateRepository @Inject constructor(
        context: Context
) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    companion object {
        private const val CURRENT_TRUSTED_CERT_FINGERPRINT_PREF = "current_trusted_certificate_fingerprint"
        private const val CURRENT_TRUSTED_CERT_HASH_TYPE_PREF = "current_trusted_certificate_hash_type"

    }

    fun updateCurTrustedCert(fingerprint: Fingerprint) {
        val base64Fingerprint = Base64.encodeToString(fingerprint.bytes, Base64.DEFAULT)
        sharedPreferences
                .edit {
                    putString(CURRENT_TRUSTED_CERT_FINGERPRINT_PREF, base64Fingerprint)
                    putString(CURRENT_TRUSTED_CERT_HASH_TYPE_PREF, fingerprint.hashType.name)
                }
    }

    fun getCurTrustedCert(): Fingerprint? {
        val base64Fingerprint = sharedPreferences.getString(
                CURRENT_TRUSTED_CERT_FINGERPRINT_PREF,
                null,
        ) ?: return null

        val hashType = sharedPreferences.getString(
                CURRENT_TRUSTED_CERT_HASH_TYPE_PREF,
                null,
        ) ?: return null

        return Fingerprint(
                bytes = Base64.decode(base64Fingerprint, Base64.DEFAULT),
                hashType = Fingerprint.HashType.valueOf(hashType)
        )
    }
}
