/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network.ssl

import org.matrix.android.sdk.api.network.ssl.Fingerprint
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

/**
 * Thrown when we are given a certificate that does match the certificate we were told to
 * expect.
 */
internal data class UnrecognizedCertificateException(
        val certificate: X509Certificate,
        val fingerprint: Fingerprint,
        override val cause: Throwable?
) : CertificateException("Unrecognized certificate with unknown fingerprint: " + certificate.subjectDN, cause)
