/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.details

import im.vector.app.core.session.clientinfo.MatrixClientInfoContent
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private const val AN_APP_NAME = "app-name"
private const val AN_APP_VERSION = "app-version"
private const val AN_APP_URL = "app-url"

class CheckIfSectionApplicationIsVisibleUseCaseTest {

    private val checkIfSectionApplicationIsVisibleUseCase = CheckIfSectionApplicationIsVisibleUseCase()

    @Test
    fun `given client info with name, version or url when checking is application section is visible then it returns true`() {
        // Given
        val clientInfoList = listOf(
                givenAClientInfo(
                        name = AN_APP_NAME,
                        version = null,
                        url = null,
                ),
                givenAClientInfo(
                        name = null,
                        version = AN_APP_VERSION,
                        url = null,
                ),
                givenAClientInfo(
                        name = null,
                        version = null,
                        url = AN_APP_URL,
                ),
                givenAClientInfo(
                        name = AN_APP_NAME,
                        version = AN_APP_VERSION,
                        url = null,
                ),
                givenAClientInfo(
                        name = AN_APP_NAME,
                        version = null,
                        url = AN_APP_URL,
                ),
                givenAClientInfo(
                        name = null,
                        version = AN_APP_VERSION,
                        url = AN_APP_URL,
                ),
                givenAClientInfo(
                        name = AN_APP_NAME,
                        version = AN_APP_VERSION,
                        url = AN_APP_URL,
                ),
        )

        clientInfoList.forEach { clientInfo ->
            // When
            val result = checkIfSectionApplicationIsVisibleUseCase.execute(clientInfo)

            // Then
            result shouldBeEqualTo true
        }
    }

    @Test
    fun `given client info with missing application info when checking is application section is visible then it returns false`() {
        // Given
        val clientInfoList = listOf(
                givenAClientInfo(
                        name = null,
                        version = null,
                        url = null,
                ),
                givenAClientInfo(
                        name = "",
                        version = null,
                        url = null,
                ),
                givenAClientInfo(
                        name = null,
                        version = "",
                        url = null,
                ),
                givenAClientInfo(
                        name = null,
                        version = null,
                        url = "",
                ),
                givenAClientInfo(
                        name = "",
                        version = "",
                        url = null,
                ),
                givenAClientInfo(
                        name = "",
                        version = null,
                        url = "",
                ),
                givenAClientInfo(
                        name = null,
                        version = "",
                        url = "",
                ),
                givenAClientInfo(
                        name = "",
                        version = "",
                        url = "",
                ),
        )

        clientInfoList.forEach { clientInfo ->
            // When
            val result = checkIfSectionApplicationIsVisibleUseCase.execute(clientInfo)

            // Then
            result shouldBeEqualTo false
        }
    }

    private fun givenAClientInfo(
            name: String?,
            version: String?,
            url: String?,
    ) = MatrixClientInfoContent(
            name = name,
            version = version,
            url = url,
    )
}
