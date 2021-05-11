/*
 * Copyright 2018 New Vector Ltd
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

import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import im.vector.app.R
import im.vector.app.features.notifications.NotificationUtils

/**
 * Tells if the application ignores battery optimizations.
 *
 * Ignoring them allows the app to run in background to make background sync with the homeserver.
 * This user option appears on Android M but Android O enforces its usage and kills apps not
 * authorised by the user to run in background.
 *
 * @param context the context
 * @return true if battery optimisations are ignored
 */
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    // no issue before Android M, battery optimisations did not exist
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
            || context.getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(context.packageName) == true
}

fun isAirplaneModeOn(context: Context): Boolean {
    return Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
}

fun isAnimationDisabled(context: Context): Boolean {
    return Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}

/**
 * display the system dialog for granting this permission. If previously granted, the
 * system will not show it (so you should call this method).
 *
 * Note: If the user finally does not grant the permission, PushManager.isBackgroundSyncAllowed()
 * will return false and the notification privacy will fallback to "LOW_DETAIL".
 */
@TargetApi(Build.VERSION_CODES.M)
fun requestDisablingBatteryOptimization(activity: Activity, activityResultLauncher: ActivityResultLauncher<Intent>) {
    val intent = Intent()
    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    intent.data = Uri.parse("package:" + activity.packageName)
    activityResultLauncher.launch(intent)
}

// ==============================================================================================================
// Clipboard helper
// ==============================================================================================================

/**
 * Copy a text to the clipboard, and display a Toast when done
 *
 * @param context the context
 * @param text    the text to copy
 */
fun copyToClipboard(context: Context, text: CharSequence, showToast: Boolean = true, @StringRes toastMessage: Int = R.string.copied_to_clipboard) {
    val clipboard = context.getSystemService<ClipboardManager>()!!
    clipboard.setPrimaryClip(ClipData.newPlainText("", text))
    if (showToast) {
        context.toast(toastMessage)
    }
}

/**
 * Shows notification settings for the current app.
 * In android O will directly opens the notification settings, in lower version it will show the App settings
 */
fun startNotificationSettingsIntent(context: Context, activityResultLauncher: ActivityResultLauncher<Intent>) {
    val intent = Intent()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        intent.putExtra("app_package", context.packageName)
        intent.putExtra("app_uid", context.applicationInfo?.uid)
    }
    activityResultLauncher.launch(intent)
}

/**
 * Shows notification system settings for the given channel id.
 */
@TargetApi(Build.VERSION_CODES.O)
fun startNotificationChannelSettingsIntent(fragment: Fragment, channelID: String) {
    if (!NotificationUtils.supportNotificationChannels()) return
    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, fragment.context?.packageName)
        putExtra(Settings.EXTRA_CHANNEL_ID, channelID)
    }
    fragment.startActivity(intent)
}

fun startAddGoogleAccountIntent(context: Context, activityResultLauncher: ActivityResultLauncher<Intent>) {
    try {
        val intent = Intent(Settings.ACTION_ADD_ACCOUNT)
        intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
        activityResultLauncher.launch(intent)
    } catch (activityNotFoundException: ActivityNotFoundException) {
        context.toast(R.string.error_no_external_application_found)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun startInstallFromSourceIntent(context: Context, activityResultLauncher: ActivityResultLauncher<Intent>) {
    try {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse(String.format("package:%s", context.packageName)))
        activityResultLauncher.launch(intent)
    } catch (activityNotFoundException: ActivityNotFoundException) {
        context.toast(R.string.error_no_external_application_found)
    }
}

fun startSharePlainTextIntent(fragment: Fragment,
                              activityResultLauncher: ActivityResultLauncher<Intent>?,
                              chooserTitle: String?,
                              text: String,
                              subject: String? = null,
                              extraTitle: String? = null) {
    val share = Intent(Intent.ACTION_SEND)
    share.type = "text/plain"
    share.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    // Add data to the intent, the receiving app will decide what to do with it.
    share.putExtra(Intent.EXTRA_SUBJECT, subject)
    share.putExtra(Intent.EXTRA_TEXT, text)

    extraTitle?.let {
        share.putExtra(Intent.EXTRA_TITLE, it)
    }

    val intent = Intent.createChooser(share, chooserTitle)
    try {
        if (activityResultLauncher != null) {
            activityResultLauncher.launch(intent)
        } else {
            fragment.startActivity(intent)
        }
    } catch (activityNotFoundException: ActivityNotFoundException) {
        fragment.activity?.toast(R.string.error_no_external_application_found)
    }
}

fun startImportTextFromFileIntent(context: Context, activityResultLauncher: ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "text/plain"
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        activityResultLauncher.launch(intent)
    } else {
        context.toast(R.string.error_no_external_application_found)
    }
}

// Not in KTX anymore
fun Context.toast(resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
}

// Not in KTX anymore
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
