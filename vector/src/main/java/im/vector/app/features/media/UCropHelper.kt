/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.media

import android.graphics.Color
import android.net.Uri
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import im.vector.app.R
import im.vector.app.core.resources.ColorProvider

fun createUCropWithDefaultSettings(colorProvider: ColorProvider,
                                   source: Uri,
                                   destination: Uri,
                                   toolbarTitle: String?): UCrop {
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
