/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.di

import javax.inject.Qualifier

/**
 * Used to inject the userId.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class UserId

/**
 * Used to inject the deviceId.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class DeviceId

/**
 * Used to inject the md5 of the userId.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class UserMd5

/**
 * Used to inject the sessionId, which is defined as md5(userId|deviceId).
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class SessionId
