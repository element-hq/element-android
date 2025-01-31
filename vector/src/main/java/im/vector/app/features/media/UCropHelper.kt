/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.media

import android.graphics.Color
import android.net.Uri
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import im.vector.app.R
import im.vector.app.core.resources.ColorProvider

fun createUCropWithDefaultSettings(
        colorProvider: ColorProvider,
        source: Uri,
        destination: Uri,
        toolbarTitle: String?
): UCrop {
    return UCrop.of(source, destination)
            .withOptions(
                    UCrop.Options()
                            .apply {
                                setAllowedGestures(
                                        /* tabScale = */ UCropActivity.SCALE,
                                        /* tabRotate = */ UCropActivity.ALL,
                                        /* tabAspectRatio = */ UCropActivity.SCALE
                                )
                                setToolbarTitle(toolbarTitle)
                                // Disable freestyle crop, usability was not easy
                                // setFreeStyleCropEnabled(true)
                                // Color used for toolbar icon and text
                                setToolbarColor(colorProvider.getColorFromAttribute(android.R.attr.colorBackground))
                                setToolbarWidgetColor(colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                                // Background
                                setRootViewBackgroundColor(colorProvider.getColorFromAttribute(android.R.attr.colorBackground))
                                // Status bar color (pb in dark mode, icon of the status bar are dark)
                                setStatusBarColor(colorProvider.getColor(R.color.android_status_bar_background_light))
                                setActiveControlsWidgetColor(colorProvider.getColorFromAttribute(R.attr.colorPrimary))
                                // Hide the logo (does not work)
                                setLogoColor(Color.TRANSPARENT)
                            }
            )
}
