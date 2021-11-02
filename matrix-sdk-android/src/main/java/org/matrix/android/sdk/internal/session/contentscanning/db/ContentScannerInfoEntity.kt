/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package org.matrix.android.sdk.internal.session.contentscanning.db

import io.realm.RealmObject

internal open class ContentScannerInfoEntity(
        var serverUrl: String? = null,
        var enabled: Boolean? = null
) : RealmObject() {

    companion object
}
