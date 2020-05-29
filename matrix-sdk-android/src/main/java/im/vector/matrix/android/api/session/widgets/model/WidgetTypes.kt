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

package im.vector.matrix.android.api.session.widgets.model

sealed class WidgetType(open val preferred: String, open val legacy: String) {
    object Jitsi : WidgetType("m.jitsi", "jitsi")
    object TradingView : WidgetType("m.tradingview", "m.tradingview")
    object Spotify : WidgetType("m.spotify", "m.spotify")
    object Video : WidgetType("m.video", "m.video")
    object GoogleDoc : WidgetType("m.googledoc", "m.googledoc")
    object GoogleCalendar : WidgetType("m.googlecalendar", "m.googlecalendar")
    object Etherpad : WidgetType("m.etherpad", "m.etherpad")
    object StickerPicker : WidgetType("m.stickerpicker", "m.stickerpicker")
    object Grafana : WidgetType("m.grafana", "m.grafana")
    object Custom : WidgetType("m.custom", "m.custom")
    object IntegrationManager : WidgetType("m.integration_manager", "m.integration_manager")
    data class Fallback(override val preferred: String, override val legacy: String) : WidgetType(preferred, legacy)

    fun matches(type: String?): Boolean {
        return type == preferred || type == legacy
    }

    fun values(): Set<String>{
        return setOf(preferred, legacy)
    }

    companion object {

        private val DEFINED_TYPES = listOf(
                Jitsi,
                TradingView,
                Spotify,
                Video,
                GoogleDoc,
                GoogleCalendar,
                Etherpad,
                StickerPicker,
                Grafana,
                Custom,
                IntegrationManager
        )

        fun fromString(type: String): WidgetType {
            val matchingType = DEFINED_TYPES.firstOrNull {
                it.matches(type)
            }
            return matchingType ?: Fallback(type, type)
        }
    }
}
