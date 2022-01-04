/*
 * Copyright 2019 New Vector Ltd
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
 * @param url     the url to open
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

fun Context.showIdentityServerConsentDialog(identityServerWithTerms: ServerAndPolicies?,
                                            consentCallBack: (() -> Unit)) {
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
