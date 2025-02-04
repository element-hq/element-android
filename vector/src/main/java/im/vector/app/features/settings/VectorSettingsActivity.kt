/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityVectorSettingsBinding
import im.vector.app.features.discovery.DiscoverySettingsFragment
import im.vector.app.features.navigation.SettingsActivityPayload
import im.vector.app.features.settings.devices.VectorSettingsDevicesFragment
import im.vector.app.features.settings.notifications.VectorSettingsNotificationFragment
import im.vector.app.features.settings.threepids.ThreePidsSettingsFragment
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import javax.inject.Inject

private const val KEY_ACTIVITY_PAYLOAD = "settings-activity-payload"

/**
 * Displays the client settings.
 */
@AndroidEntryPoint
class VectorSettingsActivity : VectorBaseActivity<ActivityVectorSettingsBinding>(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        FragmentManager.OnBackStackChangedListener,
        VectorSettingsFragmentInteractionListener {

    override fun getBinding() = ActivityVectorSettingsBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun getTitleRes() = CommonStrings.title_activity_settings

    private var keyToHighlight: String? = null

    var ignoreInvalidTokenError = false

    @Inject lateinit var session: Session

    override fun initUiAndData() {
        setupToolbar(views.settingsToolbar)
                .allowBack()

        if (isFirstCreation()) {
            // display the fragment

            when (val payload = readPayload<SettingsActivityPayload>(SettingsActivityPayload.Root)) {
                SettingsActivityPayload.General ->
                    replaceFragment(views.vectorSettingsPage, VectorSettingsGeneralFragment::class.java, null, FRAGMENT_TAG)
                SettingsActivityPayload.AdvancedSettings ->
                    replaceFragment(views.vectorSettingsPage, VectorSettingsAdvancedSettingsFragment::class.java, null, FRAGMENT_TAG)
                SettingsActivityPayload.SecurityPrivacy ->
                    replaceFragment(views.vectorSettingsPage, VectorSettingsSecurityPrivacyFragment::class.java, null, FRAGMENT_TAG)
                SettingsActivityPayload.SecurityPrivacyManageSessions -> {
                    val fragmentClass = if (vectorPreferences.isNewSessionManagerEnabled()) {
                        im.vector.app.features.settings.devices.v2.VectorSettingsDevicesFragment::class.java
                    } else {
                        VectorSettingsDevicesFragment::class.java
                    }
                    replaceFragment(
                            views.vectorSettingsPage,
                            fragmentClass,
                            null,
                            FRAGMENT_TAG
                    )
                }
                SettingsActivityPayload.Notifications -> {
                    requestHighlightPreferenceKeyOnResume(VectorPreferences.SETTINGS_ENABLE_THIS_DEVICE_PREFERENCE_KEY)
                    replaceFragment(views.vectorSettingsPage, VectorSettingsNotificationFragment::class.java, null, FRAGMENT_TAG)
                }
                is SettingsActivityPayload.DiscoverySettings -> {
                    replaceFragment(views.vectorSettingsPage, DiscoverySettingsFragment::class.java, payload, FRAGMENT_TAG)
                }
                else ->
                    replaceFragment(views.vectorSettingsPage, VectorSettingsRootFragment::class.java, null, FRAGMENT_TAG)
            }
        }

        supportFragmentManager.addOnBackStackChangedListener(this)
    }

    override fun onDestroy() {
        supportFragmentManager.removeOnBackStackChangedListener(this)
        super.onDestroy()
    }

    override fun onBackStackChanged() {
        if (0 == supportFragmentManager.backStackEntryCount) {
            supportActionBar?.title = getString(getTitleRes())
        }
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        val oFragment = try {
            pref.fragment?.let {
                supportFragmentManager.fragmentFactory.instantiate(classLoader, it)
            }
        } catch (e: Throwable) {
            showSnackbar(getString(CommonStrings.not_implemented))
            Timber.e(e)
            null
        }

        if (oFragment != null) {
            // Deprecated, I comment it, I think it is useless
            // oFragment.setTargetFragment(caller, 0)
            // Replace the existing Fragment with the new Fragment
            supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.right_in, R.anim.fade_out, R.anim.fade_in, R.anim.right_out)
                    .replace(views.vectorSettingsPage.id, oFragment, pref.title.toString())
                    .addToBackStack(null)
                    .commit()
            return true
        }
        return false
    }

    override fun requestHighlightPreferenceKeyOnResume(key: String?) {
        keyToHighlight = key
    }

    override fun requestedKeyToHighlight(): String? {
        return keyToHighlight
    }

    override fun navigateToEmailAndPhoneNumbers() {
        navigateTo(ThreePidsSettingsFragment::class.java)
    }

    override fun handleInvalidToken(globalError: GlobalError.InvalidToken) {
        if (ignoreInvalidTokenError) {
            Timber.w("Ignoring invalid token global error")
        } else {
            super.handleInvalidToken(globalError)
        }
    }

    fun <T : Fragment> navigateTo(fragmentClass: Class<T>, arguments: Bundle? = null) {
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.right_in, R.anim.fade_out, R.anim.fade_in, R.anim.right_out)
                .replace(views.vectorSettingsPage.id, fragmentClass, arguments)
                .addToBackStack(null)
                .commit()
    }

    companion object {
        fun getIntent(context: Context, directAccess: Int) = Companion.getIntent(
                context, when (directAccess) {
            EXTRA_DIRECT_ACCESS_ROOT -> SettingsActivityPayload.Root
            EXTRA_DIRECT_ACCESS_ADVANCED_SETTINGS -> SettingsActivityPayload.AdvancedSettings
            EXTRA_DIRECT_ACCESS_SECURITY_PRIVACY -> SettingsActivityPayload.SecurityPrivacy
            EXTRA_DIRECT_ACCESS_SECURITY_PRIVACY_MANAGE_SESSIONS -> SettingsActivityPayload.SecurityPrivacyManageSessions
            EXTRA_DIRECT_ACCESS_GENERAL -> SettingsActivityPayload.General
            EXTRA_DIRECT_ACCESS_NOTIFICATIONS -> SettingsActivityPayload.Notifications
            EXTRA_DIRECT_ACCESS_DISCOVERY_SETTINGS -> SettingsActivityPayload.DiscoverySettings()
            else -> {
                Timber.w("Unknown directAccess: $directAccess defaulting to Root")
                SettingsActivityPayload.Root
            }
        }
        )

        fun getIntent(context: Context, payload: SettingsActivityPayload) = Intent(context, VectorSettingsActivity::class.java)
                .applyPayload(payload)

        const val EXTRA_DIRECT_ACCESS_ROOT = 0
        const val EXTRA_DIRECT_ACCESS_ADVANCED_SETTINGS = 1
        const val EXTRA_DIRECT_ACCESS_SECURITY_PRIVACY = 2
        const val EXTRA_DIRECT_ACCESS_SECURITY_PRIVACY_MANAGE_SESSIONS = 3
        const val EXTRA_DIRECT_ACCESS_GENERAL = 4
        const val EXTRA_DIRECT_ACCESS_NOTIFICATIONS = 5
        const val EXTRA_DIRECT_ACCESS_DISCOVERY_SETTINGS = 6

        private const val FRAGMENT_TAG = "VectorSettingsPreferencesFragment"
    }
}

private inline fun <reified T : Parcelable> Activity.readPayload(default: T): T {
    return intent.getParcelableExtraCompat<T>(KEY_ACTIVITY_PAYLOAD) ?: default
}

private fun <T : Parcelable> Intent.applyPayload(payload: T): Intent {
    return putExtra(KEY_ACTIVITY_PAYLOAD, payload)
}
