/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package org.matrix.android.sdk.internal.session.contentscanning

import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.contentscanning.ContentScanApi
import javax.inject.Inject

@SessionScope
internal class ContentScanningApiProvider @Inject constructor() {
    var contentScannerApi: ContentScanApi? = null
}
