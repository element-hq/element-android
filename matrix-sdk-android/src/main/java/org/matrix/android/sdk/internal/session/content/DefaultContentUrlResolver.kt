/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.content

import org.matrix.android.sdk.api.MatrixUrls.isMxcUrl
import org.matrix.android.sdk.api.MatrixUrls.removeMxcPrefix
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.session.contentscanner.ContentScannerService
import org.matrix.android.sdk.api.session.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.internal.network.NetworkConstants
import org.matrix.android.sdk.internal.session.contentscanner.ScanEncryptorUtils
import org.matrix.android.sdk.internal.session.contentscanner.model.toJson
import org.matrix.android.sdk.internal.util.ensureTrailingSlash
import javax.inject.Inject

internal class DefaultContentUrlResolver @Inject constructor(
        homeServerConnectionConfig: HomeServerConnectionConfig,
        private val scannerService: ContentScannerService
) : ContentUrlResolver {

    private val baseUrl = homeServerConnectionConfig.homeServerUriBase.toString().ensureTrailingSlash()

    override val uploadUrl = baseUrl + NetworkConstants.URI_API_MEDIA_PREFIX_PATH_R0 + "upload"

    override fun resolveForDownload(contentUrl: String?, elementToDecrypt: ElementToDecrypt?): ContentUrlResolver.ResolvedMethod? {
        return if (scannerService.isScannerEnabled() && elementToDecrypt != null) {
            val baseUrl = scannerService.getContentScannerServer()
            val sep = if (baseUrl?.endsWith("/") == true) "" else "/"

            val url = baseUrl + sep + NetworkConstants.URI_API_PREFIX_PATH_MEDIA_PROXY_UNSTABLE + "download_encrypted"

            ContentUrlResolver.ResolvedMethod.POST(
                    url = url,
                    jsonBody = ScanEncryptorUtils
                            .getDownloadBodyAndEncryptIfNeeded(scannerService.serverPublicKey, contentUrl ?: "", elementToDecrypt)
                            .toJson()
            )
        } else {
            resolveFullSize(contentUrl)?.let { ContentUrlResolver.ResolvedMethod.GET(it) }
        }
    }

    override fun resolveFullSize(contentUrl: String?): String? {
        return contentUrl
                // do not allow non-mxc content URLs
                ?.takeIf { it.isMxcUrl() }
                ?.let {
                    resolve(
                            contentUrl = it,
                            toThumbnail = false
                    )
                }
    }

    override fun resolveThumbnail(contentUrl: String?, width: Int, height: Int, method: ContentUrlResolver.ThumbnailMethod): String? {
        return contentUrl
                // do not allow non-mxc content URLs
                ?.takeIf { it.isMxcUrl() }
                ?.let {
                    resolve(
                            contentUrl = it,
                            toThumbnail = true,
                            params = "?width=$width&height=$height&method=${method.value}"
                    )
                }
    }

    private fun resolve(contentUrl: String,
                        toThumbnail: Boolean,
                        params: String = ""): String {
        var serverAndMediaId = contentUrl.removeMxcPrefix()

        val apiPath = if (scannerService.isScannerEnabled()) {
            NetworkConstants.URI_API_PREFIX_PATH_MEDIA_PROXY_UNSTABLE
        } else {
            NetworkConstants.URI_API_MEDIA_PREFIX_PATH_R0
        }
        val prefix = if (toThumbnail) {
            apiPath + "thumbnail/"
        } else {
            apiPath + "download/"
        }
        val fragmentOffset = serverAndMediaId.indexOf("#")
        var fragment = ""
        if (fragmentOffset >= 0) {
            fragment = serverAndMediaId.substring(fragmentOffset)
            serverAndMediaId = serverAndMediaId.substring(0, fragmentOffset)
        }

        val resolvedUrl = if (scannerService.isScannerEnabled()) {
            scannerService.getContentScannerServer()!!.ensureTrailingSlash()
        } else {
            baseUrl
        }
        return resolvedUrl + prefix + serverAndMediaId + params + fragment
    }
}
