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

package im.vector.app.features.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import im.vector.app.R
import im.vector.app.databinding.ActivityTestLinkifyBinding
import im.vector.app.databinding.ItemTestLinkifyBinding

class TestLinkifyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val views = ActivityTestLinkifyBinding.inflate(layoutInflater)
        setContentView(views.root)
        views.testLinkifyContentView.removeAllViews()

        listOf(
                "https://www.html5rocks.com/en/tutorials/webrtc/basics/ |",
                "https://www.html5rocks.com/en/tutorials/webrtc/basics/",
                "mailto mailto:test@toto.com  test@toto.com",
                "Here is the link.www.test.com/foo/?23=35 you got it?",
                "www.lemonde.fr",
                " /www.lemonde.fr",
                "://www.lemonde.fr",
                "file:///dev/null ",
                " ansible/xoxys.matrix#2c0b65eb",
                "foo.ansible/xoxys.matrix#2c0b65eb",
                "foo.ansible.fpo/xoxys.matrix#2c0b65eb",
                "https://foo.ansible.fpo/xoxys.matrix#2c0b65eb",
                "@vf:matrix.org",
                "+44 207 123 1234",
                "+33141437940",
                "1234",
                "3456.34,089",
                "ksks9808",
                "For example: geo:48.85828,2.29449?z=16 should be clickable",
                "geo:37.786971,-122.399677;u=35",
                "37.786971,-122.399677;u=35",
                "48.107864,-1.712153",
                "synchrone peut tenir la route la",
                "that.is.some.sexy.link",
                "test overlap 48.107864,0673728392 geo + pn?",
                "test overlap 0673728392,48.107864 geo + pn?",
                "If I add a link in brackets like (help for Riot: https://about.riot.im/help), the link is usable on Riot for Desktop",
                "(help for Riot: https://about.riot.im/help)",
                "http://example.com/test(1).html",
                "http://example.com/test(1)",
                "https://about.riot.im/help)",
                "(http://example.com/test(1))",
                "http://example.com/test1)",
                "http://example.com/test1/, et ca",
                "www.example.com/, et ca",
                "foo.ansible.toplevel/xoxys.matrix#2c0b65eb",
                "foo.ansible.ninja/xoxys.matrix#2c0b65eb",
                "in brackets like (help for Riot: https://www.exemple/com/find(1)) , the link is usable ",
                """
                    In brackets like (help for Riot: https://about.riot.im/help) , the link is usable,
                    But you can call +44 207 123 1234 and come to 37.786971,-122.399677;u=35 then
                    see if this mail jhon@riot.im is active but this should not 12345
                """.trimIndent()
        )
                .forEach { textContent ->
                    val item = LayoutInflater.from(this)
                            .inflate(R.layout.item_test_linkify, views.testLinkifyContentView, false)
                    val subViews = ItemTestLinkifyBinding.bind(item)
                    subViews.testLinkifyAutoText.apply {
                        text = textContent
                        /* TODO Use BetterLinkMovementMethod when the other PR is merged
                        movementMethod = MatrixLinkMovementMethod(object : MockMessageAdapterActionListener() {
                            override fun onURLClick(uri: Uri?) {
                                Snackbar.make(coordinatorLayout, "URI Clicked: $uri", Snackbar.LENGTH_LONG)
                                        .setAction("open") {
                                            openUrlInExternalBrowser(this@TestLinkifyActivity, uri)
                                        }
                                        .show()
                            }
                        })
                         */
                    }

                    subViews.testLinkifyCustomText.apply {
                        text = textContent
                        /* TODO Use BetterLinkMovementMethod when the other PR is merged
                        movementMethod = MatrixLinkMovementMethod(object : MockMessageAdapterActionListener() {
                            override fun onURLClick(uri: Uri?) {
                                Snackbar.make(coordinatorLayout, "URI Clicked: $uri", Snackbar.LENGTH_LONG)
                                        .setAction("open") {
                                            openUrlInExternalBrowser(this@TestLinkifyActivity, uri)
                                        }
                                        .show()
                            }
                        })
                         */

                        // TODO Call VectorLinkify.addLinks(text)
                    }

                    views.testLinkifyContentView
                            .addView(item, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }
    }
}
