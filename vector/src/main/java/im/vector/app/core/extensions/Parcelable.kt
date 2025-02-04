/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import android.os.Bundle
import android.os.Parcelable
import com.airbnb.mvrx.Mavericks

fun Parcelable?.toMvRxBundle(): Bundle? {
    return this?.let { Bundle().apply { putParcelable(Mavericks.KEY_ARG, it) } }
}
