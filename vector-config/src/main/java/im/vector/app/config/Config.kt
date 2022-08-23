/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.config

/**
 * Set of flags to configure the application.
 */
object Config {
    /**
     * Flag to allow external UnifiedPush distributors to be chosen by the user.
     *
     * Set to true to allow any available external UnifiedPush distributor to be chosen by the user.
     * - For Gplay variant it means that FCM will be used by default, but user can choose another UnifiedPush distributor;
     * - For F-Droid variant, it means that background polling will be used by default, but user can choose another UnifiedPush distributor.
     *
     * Set to false to prevent usage of external UnifiedPush distributors.
     * - For Gplay variant it means that only FCM will be used;
     * - For F-Droid variant, it means that only background polling will be available to the user.
     *
     * *Note*: When the app is already installed on users' phone:
     * - Changing the value from `false` to `true` will let the user be able to select an external UnifiedPush distributor;
     * - Changing the value from `true` to `false` will force the app to return to the background sync / Firebase Push.
     */
    const val ALLOW_EXTERNAL_UNIFIED_PUSH_DISTRIBUTORS = true

    const val ENABLE_LOCATION_SHARING = true
    const val LOCATION_MAP_TILER_KEY = "fU3vlMsMn4Jb6dnEIFsx"

    /**
     * The maximum length of voice messages in milliseconds.
     */
    const val VOICE_MESSAGE_LIMIT_MS = 120_000L

    /**
     * The strategy for sharing device keys.
     */
    val KEY_SHARING_STRATEGY = KeySharingStrategy.WhenTyping

    /**
     * The onboarding flow.
     */
    val ONBOARDING_VARIANT = OnboardingVariant.FTUE_AUTH

    /**
     * If set, MSC3086 asserted identity messages sent on VoIP calls will cause the call to appear in the room corresponding to the asserted identity.
     * This *must* only be set in trusted environments.
     */
    const val HANDLE_CALL_ASSERTED_IDENTITY_EVENTS = false

    const val LOW_PRIVACY_LOG_ENABLE = false
    const val ENABLE_STRICT_MODE_LOGS = false

    /**
     * The analytics configuration to use for the Debug build type.
     * Can be disabled by providing Analytics.Disabled
     */
    val DEBUG_ANALYTICS_CONFIG = Analytics.PostHog(
            postHogHost = "https://posthog.element.dev",
            postHogApiKey = "phc_VtA1L35nw3aeAtHIx1ayrGdzGkss7k1xINeXcoIQzXN",
            policyLink = "https://element.io/cookie-policy",
    )

    /**
     * The analytics configuration to use for the Release build type.
     * Can be disabled by providing Analytics.Disabled
     */
    val RELEASE_ANALYTICS_CONFIG = Analytics.PostHog(
            postHogHost = "https://posthog.hss.element.io",
            postHogApiKey = "phc_Jzsm6DTm6V2705zeU5dcNvQDlonOR68XvX2sh1sEOHO",
            policyLink = "https://element.io/cookie-policy",
    )

    /**
     * The analytics configuration to use for the Nightly build type.
     * Can be disabled by providing Analytics.Disabled
     */
    val NIGHTLY_ANALYTICS_CONFIG = RELEASE_ANALYTICS_CONFIG
}
