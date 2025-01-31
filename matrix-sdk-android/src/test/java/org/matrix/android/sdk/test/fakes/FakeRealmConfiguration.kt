/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fakes

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.realm.Realm
import io.realm.RealmConfiguration
import org.matrix.android.sdk.internal.database.awaitTransaction

internal class FakeRealmConfiguration {

    init {
        mockkStatic("org.matrix.android.sdk.internal.database.AsyncTransactionKt")
    }

    val instance = mockk<RealmConfiguration>()

    fun <T> givenAwaitTransaction(realm: Realm) {
        val transaction = slot<suspend (Realm) -> T>()
        coEvery { awaitTransaction(instance, capture(transaction)) } coAnswers {
            secondArg<suspend (Realm) -> T>().invoke(realm)
        }
    }
}
