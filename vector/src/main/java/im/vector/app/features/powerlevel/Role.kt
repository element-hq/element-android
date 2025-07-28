/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.powerlevel

import org.matrix.android.sdk.api.session.room.powerlevels.Role

fun Role.isOwner() = this == Role.Creator || this == Role.SuperAdmin
