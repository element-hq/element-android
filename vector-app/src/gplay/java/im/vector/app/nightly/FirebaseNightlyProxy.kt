/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.nightly

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.appdistribution.FirebaseAppDistributionException
import im.vector.app.core.di.DefaultPreferences
import im.vector.app.core.resources.BuildMeta
import im.vector.app.features.home.NightlyProxy
import im.vector.lib.core.utils.timer.Clock
import timber.log.Timber
import javax.inject.Inject

class FirebaseNightlyProxy @Inject constructor(
        private val clock: Clock,
        @DefaultPreferences
        private val sharedPreferences: SharedPreferences,
        private val buildMeta: BuildMeta,
) : NightlyProxy {

    override fun isNightlyBuild(): Boolean {
        return buildMeta.applicationId in nightlyPackages
    }

    override fun updateApplication() {
        val firebaseAppDistribution = FirebaseAppDistribution.getInstance()
        firebaseAppDistribution.updateIfNewReleaseAvailable()
                .addOnProgressListener { up ->
                    Timber.d("FirebaseAppDistribution progress: ${up.updateStatus}. ${up.apkBytesDownloaded}/${up.apkFileTotalBytes}")
                }
                .addOnFailureListener { e ->
                    if (e is FirebaseAppDistributionException) {
                        when (e.errorCode) {
                            FirebaseAppDistributionException.Status.NOT_IMPLEMENTED -> {
                                // SDK did nothing. This is expected when building for Play.
                                Timber.d("FirebaseAppDistribution NOT_IMPLEMENTED error")
                            }
                            else -> {
                                // Handle other errors.
                                Timber.e(e, "FirebaseAppDistribution error, status: ${e.errorCode}")
                            }
                        }
                    } else {
                        Timber.e(e, "FirebaseAppDistribution - other error")
                    }
                }
                .addOnSuccessListener {
                    Timber.d("FirebaseAppDistribution Success!")
                }
    }

    override fun canDisplayPopup(): Boolean {
        if (!POPUP_IS_ENABLED) return false
        if (!isNightlyBuild()) return false
        val today = clock.epochMillis() / A_DAY_IN_MILLIS
        val lastDisplayPopupDay = sharedPreferences.getLong(SHARED_PREF_KEY, 0)
        return (today > lastDisplayPopupDay)
                .also { canDisplayPopup ->
                    if (canDisplayPopup) {
                        sharedPreferences.edit {
                            putLong(SHARED_PREF_KEY, today)
                        }
                    }
                }
    }

    companion object {
        private const val POPUP_IS_ENABLED = false
        private const val A_DAY_IN_MILLIS = 8_600_000L
        private const val SHARED_PREF_KEY = "LAST_NIGHTLY_POPUP_DAY"

        private val nightlyPackages = listOf(
                "im.vector.app.nightly"
        )
    }
}
