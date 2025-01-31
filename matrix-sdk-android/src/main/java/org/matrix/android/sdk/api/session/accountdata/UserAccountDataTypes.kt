/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.accountdata

object UserAccountDataTypes {
    const val TYPE_IGNORED_USER_LIST = "m.ignored_user_list"
    const val TYPE_DIRECT_MESSAGES = "m.direct"
    const val TYPE_BREADCRUMBS = "im.vector.setting.breadcrumbs"
    const val TYPE_PREVIEW_URLS = "org.matrix.preview_urls"
    const val TYPE_WIDGETS = "m.widgets"
    const val TYPE_PUSH_RULES = "m.push_rules"
    const val TYPE_INTEGRATION_PROVISIONING = "im.vector.setting.integration_provisioning"
    const val TYPE_ALLOWED_WIDGETS = "im.vector.setting.allowed_widgets"
    const val TYPE_IDENTITY_SERVER = "m.identity_server"
    const val TYPE_ACCEPTED_TERMS = "m.accepted_terms"
    const val TYPE_OVERRIDE_COLORS = "im.vector.setting.override_colors"
}
