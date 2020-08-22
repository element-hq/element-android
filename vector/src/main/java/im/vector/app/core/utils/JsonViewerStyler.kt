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

package im.vector.app.core.utils

import im.vector.app.R
import im.vector.app.core.resources.ColorProvider
import org.billcarsonfr.jsonviewer.JSonViewerStyleProvider

fun createJSonViewerStyleProvider(colorProvider: ColorProvider): JSonViewerStyleProvider {
    return JSonViewerStyleProvider(
            keyColor = colorProvider.getColor(R.color.riotx_accent),
            secondaryColor = colorProvider.getColorFromAttribute(R.attr.riotx_text_secondary),
            stringColor = colorProvider.getColorFromAttribute(R.attr.vctr_notice_text_color),
            baseColor = colorProvider.getColorFromAttribute(R.attr.riotx_text_primary),
            booleanColor = colorProvider.getColorFromAttribute(R.attr.vctr_notice_text_color),
            numberColor = colorProvider.getColorFromAttribute(R.attr.vctr_notice_text_color)
    )
}
