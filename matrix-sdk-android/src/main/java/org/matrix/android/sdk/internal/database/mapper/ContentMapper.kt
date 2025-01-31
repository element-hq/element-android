/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.util.JSON_DICT_PARAMETERIZED_TYPE
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.network.parsing.CheckNumberType

internal object ContentMapper {

    private val moshi = MoshiProvider.providesMoshi()
    private val castJsonNumberMoshi by lazy {
        // We are adding the CheckNumberType as we are serializing/deserializing multiple time in a row
        // and we lost typing information doing so.
        // We don't want this check to be done on all adapters, so we create a new moshi just for that.
        MoshiProvider.providesMoshi()
                .newBuilder()
                .add(CheckNumberType.JSON_ADAPTER_FACTORY)
                .build()
    }

    fun map(content: String?, castJsonNumbers: Boolean = false): Content? {
        return content?.let {
            if (castJsonNumbers) {
                castJsonNumberMoshi
            } else {
                moshi
            }.adapter<Content>(JSON_DICT_PARAMETERIZED_TYPE).fromJson(it)
        }
    }

    fun map(content: Content?): String? {
        return content?.let {
            moshi.adapter<Content>(JSON_DICT_PARAMETERIZED_TYPE).toJson(it)
        }
    }
}
