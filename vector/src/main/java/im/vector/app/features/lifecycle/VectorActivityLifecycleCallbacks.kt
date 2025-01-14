/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.lifecycle

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.getSystemService
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.popup.PopupAlertManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.util.getPackageInfoCompat
import timber.log.Timber

class VectorActivityLifecycleCallbacks constructor(private val popupAlertManager: PopupAlertManager) : Application.ActivityLifecycleCallbacks {
    /**
     * The activities information collected from the app manifest.
     */
    private var activitiesInfo: Array<ActivityInfo> = emptyArray()

    private val coroutineScope = CoroutineScope(SupervisorJob())

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        popupAlertManager.onNewActivityDisplayed(activity)
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activitiesInfo.isEmpty()) {
            val context = activity.applicationContext
            val packageManager: PackageManager = context.packageManager

            // Get all activities from element android
            activitiesInfo = packageManager.getPackageInfoCompat(context.packageName, PackageManager.GET_ACTIVITIES).activities

            // Get all activities from PermissionController module
            // See https://source.android.com/docs/core/architecture/modular-system/permissioncontroller#package-format
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
                activitiesInfo += tryOrNull {
                    packageManager.getPackageInfoCompat("com.google.android.permissioncontroller", PackageManager.GET_ACTIVITIES).activities
                } ?: tryOrNull {
                    packageManager.getModuleInfo("com.google.android.permission", 1).packageName?.let {
                        packageManager.getPackageInfoCompat(it, PackageManager.GET_ACTIVITIES or PackageManager.MATCH_APEX).activities
                    }
                }.orEmpty()
            }
        }

        // restart the app if the task contains an unknown activity
        coroutineScope.launch {
            val isTaskCorrupted = try {
                isTaskCorrupted(activity)
            } catch (failure: Throwable) {
                when (failure) {
                    // The task was not found. We can ignore it.
                    is IllegalArgumentException -> {
                        Timber.e("The task was not found: ${failure.localizedMessage}")
                        false
                    }
                    is PackageManager.NameNotFoundException -> {
                        Timber.e("Package manager error: ${failure.localizedMessage}")
                        true
                    }
                    else -> throw failure
                }
            }

            if (isTaskCorrupted) {
                Timber.e("Application is potentially corrupted by an unknown activity")
                MainActivity.restartApp(activity, MainActivityArgs())
                return@launch
            }
        }
    }

    /**
     * Check if all activities running on the task with package name affinity are safe.
     *
     * @return true if an app task is corrupted by a potentially malicious activity
     */
    private suspend fun isTaskCorrupted(activity: Activity): Boolean = withContext(Dispatchers.Default) {
        val context = activity.applicationContext

        // Get all running activities on app task
        // and compare to activities declared in manifest
        val manager = context.getSystemService<ActivityManager>() ?: return@withContext false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android lint may return an error on topActivity field.
            // This field was added in ActivityManager.RecentTaskInfo class since Android M (API level 23)
            // and it is inherited from TaskInfo since Android Q (API level 29).
            // API 23 changes : https://developer.android.com/sdk/api_diff/23/changes/android.app.ActivityManager.RecentTaskInfo
            // API 29 changes : https://developer.android.com/sdk/api_diff/29/changes/android.app.ActivityManager.RecentTaskInfo
            manager.appTasks.any { appTask ->
                appTask.taskInfo.topActivity?.let { isPotentialMaliciousActivity(it) } ?: false
            }
        } else {
            // Android lint may return an error on topActivity field.
            // This was present in ActivityManager.RunningTaskInfo class since API level 1!
            // and it is inherited from TaskInfo since Android Q (API level 29).
            // API 29 changes : https://developer.android.com/sdk/api_diff/29/changes/android.app.ActivityManager.RunningTaskInfo
            @Suppress("DEPRECATION")
            manager.getRunningTasks(10).any { runningTaskInfo ->
                runningTaskInfo.topActivity?.let {
                    // Check whether the activity task affinity matches with app task affinity.
                    // The activity is considered safe when its task affinity doesn't correspond to app task affinity.
                    if (context.packageManager.getActivityInfo(it, 0).taskAffinity == context.applicationInfo.taskAffinity) {
                        isPotentialMaliciousActivity(it)
                    } else false
                } ?: false
            }
        }
    }

    /**
     * Detect potential malicious activity.
     * Check if the activity running in app task is declared in app manifest.
     *
     * @param activity the activity of the task
     * @return true if the activity is potentially malicious
     */
    private fun isPotentialMaliciousActivity(activity: ComponentName): Boolean = activitiesInfo.none { it.name == activity.className }
}
