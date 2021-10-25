/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package im.vector.matrix.android.sdk.internal.session.contentscanning.db

import io.realm.annotations.RealmModule

/**
 * Realm module for content scanner classes
 */
@RealmModule(library = true,
        classes = [
            ContentScannerInfoEntity::class,
            ContentScanResultEntity::class
        ])
internal class ContentScannerRealmModule
