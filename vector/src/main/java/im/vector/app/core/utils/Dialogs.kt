/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import android.content.Context
import android.text.method.LinkMovementMethod
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.features.discovery.ServerAndPolicies
import me.gujun.android.span.link
import me.gujun.android.span.span

/**
 * Open a web view above the current activity.
 *
 * @param url the url to open
 */
fun Context.displayInWebView(url: String) {
    val wv = WebView(this)

    // Set a WebViewClient to ensure redirection is handled directly in the WebView
    wv.webViewClient = WebViewClient()

    wv.loadUrl(url)
    MaterialAlertDialogBuilder(this)
            .setView(wv)
            .setPositiveButton(android.R.string.ok, null)
            .show()
}

fun Context.showIdentityServerConsentDialog(
        identityServerWithTerms: ServerAndPolicies?,
        consentCallBack: (() -> Unit)
) {
    // Build the message
    val content = span {
        +getString(R.string.identity_server_consent_dialog_content_3)
        +"\n\n"
        if (identityServerWithTerms?.policies?.isNullOrEmpty() == false) {
            span {
                textStyle = "bold"
                text = getString(R.string.settings_privacy_policy)
            }
            identityServerWithTerms.policies.forEach {
                +"\n • "
                // Use the url as the text too
                link(it.url, it.url)
            }
            +"\n\n"
        }
        +getString(R.string.identity_server_consent_dialog_content_question)
    }
    MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.identity_server_consent_dialog_title_2, identityServerWithTerms?.serverUrl.orEmpty()))
            .setMessage(content)
            .setPositiveButton(R.string.action_agree) { _, _ ->
                consentCallBack.invoke()
            }
            .setNegativeButton(R.string.action_not_now, null)
            .show()
            .apply {
                // Make the link(s) clickable. Must be called after show()
                (findViewById(android.R.id.message) as? TextView)?.movementMethod = LinkMovementMethod.getInstance()
            }
}
