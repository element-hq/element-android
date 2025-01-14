/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.push

import androidx.fragment.app.Fragment
import im.vector.app.features.settings.troubleshoot.NotificationTroubleshootTestManager

interface NotificationTroubleshootTestManagerFactory {
    fun create(fragment: Fragment): NotificationTroubleshootTestManager
}
