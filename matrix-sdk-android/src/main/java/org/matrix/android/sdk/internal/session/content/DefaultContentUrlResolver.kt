/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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

    private fun resolve(
            contentUrl: String,
            toThumbnail: Boolean,
            params: String = ""
    ): String {
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
