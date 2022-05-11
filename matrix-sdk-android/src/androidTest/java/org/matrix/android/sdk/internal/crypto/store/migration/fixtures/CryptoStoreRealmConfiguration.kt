/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.crypto.store.migration.fixtures

import io.realm.RealmConfiguration
import org.matrix.android.sdk.common.TemporaryRealmConfigurationFactory
import org.matrix.android.sdk.internal.crypto.store.db.RealmCryptoStoreMigration
import org.matrix.android.sdk.internal.crypto.store.db.RealmCryptoStoreModule

fun TemporaryRealmConfigurationFactory.configurationForMigrationFrom15To16(populateCryptoStore: Boolean): RealmConfiguration {
    return create(
            realmFilename = "crypto_store.realm",
            assetFilename = "crypto_store_migration_15_to_16.realm".takeIf { populateCryptoStore },
            schemaVersion = 16L,
            module = RealmCryptoStoreModule(),
            migration = RealmCryptoStoreMigration(root)
    )
}
