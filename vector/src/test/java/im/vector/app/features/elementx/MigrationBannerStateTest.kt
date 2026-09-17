/*
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.elementx

import im.vector.app.features.raw.wellknown.ElementWellKnown
import im.vector.app.features.raw.wellknown.MigrationBanner
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fakes.toTestString
import im.vector.lib.strings.R
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Test
import java.time.Instant

private const val A_TITLE = "Time to move"
private const val A_BODY = "Get the new app at https://element.io/download"
private const val A_BUTTON_TEXT = "Get Element Pro"
private const val ANOTHER_APP_ID = "io.element.pro"

class MigrationBannerStateTest {

    private val fakeStringProvider = FakeStringProvider()

    /**
     * Compute the state from a .well-known file which may or may not contain a migration banner section.
     */
    private fun computeState(
            migrationBanner: MigrationBanner?,
            useDefaultOnMissingData: Boolean = false,
    ) = ElementWellKnown(migrationBanner = migrationBanner)
            .toMigrationBannerState(
                    stringProvider = fakeStringProvider.instance,
                    useDefaultOnMissingData = useDefaultOnMissingData,
            )

    /**
     * Compute the state when there is no .well-known file at all, or when it could not be parsed.
     */
    private fun computeStateWithoutWellKnown(
            useDefaultOnMissingData: Boolean = false,
    ) = null.toMigrationBannerState(
            stringProvider = fakeStringProvider.instance,
            useDefaultOnMissingData = useDefaultOnMissingData,
    )

    private fun defaultShowState(targetAppId: String = DEFAULT_TARGET_APP_ID) = MigrationBannerState.Show(
            title = R.string.view_element_x_banner_title.toTestString(),
            body = R.string.view_element_x_banner_body.toTestString(),
            showButton = true,
            buttonText = R.string.view_element_x_banner_button.toTestString(),
            targetAppId = targetAppId,
    )

    @Test
    fun `given no migration banner data and defaults are disabled when computing state then banner is hidden`() {
        computeState(null) shouldBeEqualTo MigrationBannerState.Hide
    }

    @Test
    fun `given no migration banner data and defaults are enabled when computing state then banner is shown with defaults`() {
        computeState(null, useDefaultOnMissingData = true) shouldBeEqualTo defaultShowState()
    }

    @Test
    fun `given no well known data and defaults are disabled when computing state then banner is hidden`() {
        computeStateWithoutWellKnown() shouldBeEqualTo MigrationBannerState.Hide
    }

    @Test
    fun `given no well known data and defaults are enabled when computing state then banner is shown with defaults`() {
        computeStateWithoutWellKnown(useDefaultOnMissingData = true) shouldBeEqualTo defaultShowState()
    }

    @Test
    fun `given migration banner is explicitly disabled when computing state then banner is hidden`() {
        computeState(MigrationBanner(enabled = false)) shouldBeEqualTo MigrationBannerState.Hide
    }

    @Test
    fun `given migration banner is explicitly disabled and defaults are enabled when computing state then banner is hidden`() {
        computeState(MigrationBanner(enabled = false), useDefaultOnMissingData = true) shouldBeEqualTo MigrationBannerState.Hide
    }

    @Test
    fun `given migration banner with no customisation when computing state then defaults are used`() {
        computeState(MigrationBanner()) shouldBeEqualTo defaultShowState()
    }

    @Test
    fun `given migration banner with custom content when computing state then content is used as is`() {
        val state = computeState(
                MigrationBanner(
                        title = A_TITLE,
                        body = A_BODY,
                        buttonText = A_BUTTON_TEXT,
                        targetAppIdAndroid = ANOTHER_APP_ID,
                )
        )

        state shouldBeEqualTo MigrationBannerState.Show(
                title = A_TITLE,
                body = A_BODY,
                showButton = true,
                buttonText = A_BUTTON_TEXT,
                targetAppId = ANOTHER_APP_ID,
        )
    }

    @Test
    fun `given migration banner with blank custom content when computing state then defaults are used`() {
        computeState(MigrationBanner(title = "  ", body = "")) shouldBeEqualTo defaultShowState()
    }

    @Test
    fun `given a blank target app id when computing state then no button is displayed`() {
        val state = computeState(MigrationBanner(targetAppIdAndroid = "", buttonText = A_BUTTON_TEXT))

        state.shouldBeInstanceOf<MigrationBannerState.Show>().showButton shouldBeEqualTo false
    }

    @Test
    fun `given a blank button text when computing state then the button is displayed with the default text`() {
        computeState(MigrationBanner(buttonText = "", targetAppIdAndroid = ANOTHER_APP_ID)) shouldBeEqualTo
                defaultShowState(targetAppId = ANOTHER_APP_ID)
    }

    @Test
    fun `given the default target app id when computing state then the Element X icon is displayed`() {
        val state = computeState(MigrationBanner())

        state.shouldBeInstanceOf<MigrationBannerState.Show>().isElementX shouldBeEqualTo true
    }

    @Test
    fun `given a custom target app id when computing state then the Element X icon is not displayed`() {
        val state = computeState(MigrationBanner(targetAppIdAndroid = ANOTHER_APP_ID))

        state.shouldBeInstanceOf<MigrationBannerState.Show>().isElementX shouldBeEqualTo false
    }

    @Test
    fun `given the force showing date then it matches the 15th of November 2026`() {
        MigrationBannerViewModel.MIGRATION_BANNER_SHOW_WHEN_NOT_CONFIGURED_START_DATE shouldBeEqualTo
                Instant.parse("2026-11-15T00:00:00Z").toEpochMilli()
    }
}
