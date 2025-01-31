/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync

import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.task.SemaphoreCoroutineSequencer
import javax.inject.Inject

@SessionScope
internal class SyncTaskSequencer @Inject constructor() : SemaphoreCoroutineSequencer()
