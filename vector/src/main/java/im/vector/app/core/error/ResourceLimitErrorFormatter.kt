/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.error

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import im.vector.lib.strings.CommonStrings
import me.gujun.android.span.span
import org.matrix.android.sdk.api.failure.MatrixError

class ResourceLimitErrorFormatter(private val context: Context) {

    // 'hard' if the logged in user has been locked out, 'soft' if they haven't
    sealed class Mode(@StringRes val mauErrorRes: Int, @StringRes val defaultErrorRes: Int, @StringRes val contactRes: Int) {
        // User can still send message (will be used in a near future)
        object Soft : Mode(CommonStrings.resource_limit_soft_mau, CommonStrings.resource_limit_soft_default, CommonStrings.resource_limit_soft_contact)

        // User cannot send message anymore
        object Hard : Mode(CommonStrings.resource_limit_hard_mau, CommonStrings.resource_limit_hard_default, CommonStrings.resource_limit_hard_contact)
    }

    fun format(
            matrixError: MatrixError,
            mode: Mode,
            separator: CharSequence = " ",
            clickable: Boolean = false
    ): CharSequence {
        val error = if (MatrixError.LIMIT_TYPE_MAU == matrixError.limitType) {
            context.getString(mode.mauErrorRes)
        } else {
            context.getString(mode.defaultErrorRes)
        }
        val contact = if (clickable && matrixError.adminUri != null) {
            val contactSubString = uriAsLink(matrixError.adminUri!!)
            val contactFullString = context.getString(mode.contactRes, contactSubString)
            HtmlCompat.fromHtml(contactFullString, HtmlCompat.FROM_HTML_MODE_LEGACY)
        } else {
            val contactSubString = context.getString(CommonStrings.resource_limit_contact_admin)
            context.getString(mode.contactRes, contactSubString)
        }
        return span {
            text = error
        }
                .append(separator)
                .append(contact)
    }

    /**
     * Create a HTML link with a uri.
     */
    private fun uriAsLink(uri: String): String {
        val contactStr = context.getString(CommonStrings.resource_limit_contact_admin)
        return "<a href=\"$uri\">$contactStr</a>"
    }
}
