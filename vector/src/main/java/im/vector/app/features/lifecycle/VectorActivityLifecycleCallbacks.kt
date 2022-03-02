/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.lifecycle

import android.annotation.SuppressLint
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
        // restart the app if the task contains an unknown activity
        coroutineScope.launch {
            val isTaskCorrupted = try {
                isTaskCorrupted(activity)
            } catch (failure: Throwable) {
                when (failure) {
                    // The task was not found. We can ignore it.
                    is IllegalArgumentException             -> {
                        Timber.e("The task was not found: ${failure.localizedMessage}")
                        false
                    }
                    is PackageManager.NameNotFoundException -> {
                        Timber.e("Package manager error: ${failure.localizedMessage}")
                        true
                    }
                    else                                    -> throw failure
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
    @SuppressLint("NewApi")
    @Suppress("DEPRECATION")
    private suspend fun isTaskCorrupted(activity: Activity): Boolean = withContext(Dispatchers.Default) {
        val context = activity.applicationContext
        val packageManager: PackageManager = context.packageManager

        // Get all activities from app manifest
        if (activitiesInfo.isEmpty()) {
            activitiesInfo = packageManager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES).activities
        }

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
            manager.getRunningTasks(10).any { runningTaskInfo ->
                runningTaskInfo.topActivity?.let {
                    // Check whether the activity task affinity matches with app task affinity.
                    // The activity is considered safe when its task affinity doesn't correspond to app task affinity.
                    if (packageManager.getActivityInfo(it, 0).taskAffinity == context.applicationInfo.taskAffinity) {
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
     * @param activity      the activity of the task
     * @return true if the activity is potentially malicious
     */
    private fun isPotentialMaliciousActivity(activity: ComponentName): Boolean = activitiesInfo.none { it.name == activity.className }
}
