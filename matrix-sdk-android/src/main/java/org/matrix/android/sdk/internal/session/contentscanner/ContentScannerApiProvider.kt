/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package org.matrix.android.sdk.internal.session.contentscanner

import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class ContentScannerApiProvider @Inject constructor() {
    var contentScannerApi: ContentScannerApi? = null
}
