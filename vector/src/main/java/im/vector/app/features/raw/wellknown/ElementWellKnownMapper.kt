/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.raw.wellknown

import com.squareup.moshi.JsonAdapter
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.util.MatrixJsonParser

object ElementWellKnownMapper {

    val adapter: JsonAdapter<ElementWellKnown> = MatrixJsonParser.getMoshi().adapter(ElementWellKnown::class.java)

    fun from(value: String): ElementWellKnown? {
        return tryOrNull("Unable to parse well-known data") { adapter.fromJson(value) }
    }
}
