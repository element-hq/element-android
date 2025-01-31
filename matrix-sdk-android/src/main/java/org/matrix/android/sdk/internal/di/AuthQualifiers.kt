/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class Authenticated

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class AuthenticatedIdentity

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class Unauthenticated

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class UnauthenticatedWithCertificate

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class UnauthenticatedWithCertificateWithProgress
