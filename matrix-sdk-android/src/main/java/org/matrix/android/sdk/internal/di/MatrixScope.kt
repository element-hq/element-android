/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.di

import javax.inject.Scope

/**
 * Use the annotation @MatrixScope to annotate classes we want the SDK to instantiate only once.
 */
@Scope
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
internal annotation class MatrixScope
