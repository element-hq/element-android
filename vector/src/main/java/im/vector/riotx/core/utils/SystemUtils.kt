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

package im.vector.riotx.core.utils

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
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import im.vector.riotx.R
import im.vector.riotx.features.notifications.supportNotificationChannels
import im.vector.riotx.features.settings.VectorLocale
import timber.log.Timber
import java.util.*

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
           || (context.getSystemService(Context.POWER_SERVICE) as PowerManager?)?.isIgnoringBatteryOptimizations(context.packageName) == true
}

/**
 * display the system dialog for granting this permission. If previously granted, the
 * system will not show it (so you should call this method).
 *
 * Note: If the user finally does not grant the permission, PushManager.isBackgroundSyncAllowed()
 * will return false and the notification privacy will fallback to "LOW_DETAIL".
 */
@TargetApi(Build.VERSION_CODES.M)
fun requestDisablingBatteryOptimization(activity: Activity, fragment: Fragment?, requestCode: Int) {
    val intent = Intent()
    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    intent.data = Uri.parse("package:" + activity.packageName)
    if (fragment != null) {
        fragment.startActivityForResult(intent, requestCode)
    } else {
        activity.startActivityForResult(intent, requestCode)
    }
}

//==============================================================================================================
// Clipboard helper
//==============================================================================================================

/**
 * Copy a text to the clipboard, and display a Toast when done
 *
 * @param context the context
 * @param text    the text to copy
 */
fun copyToClipboard(context: Context, text: CharSequence, showToast: Boolean = true, @StringRes toastMessage : Int = R.string.copied_to_clipboard) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.primaryClip = ClipData.newPlainText("", text)
    if (showToast) {
        context.toast(toastMessage)
    }
}

/**
 * Provides the device locale
 *
 * @return the device locale
 */
fun getDeviceLocale(context: Context): Locale {
    var locale: Locale

    locale = try {
        val packageManager = context.packageManager
        val resources = packageManager.getResourcesForApplication("android")
        resources.configuration.locale
    } catch (e: Exception) {
        Timber.e(e, "## getDeviceLocale() failed " + e.message)
        // Fallback to application locale
        VectorLocale.applicationLocale
    }

    return locale
}

/**
 * Shows notification settings for the current app.
 * In android O will directly opens the notification settings, in lower version it will show the App settings
 */
fun startNotificationSettingsIntent(activity: AppCompatActivity, requestCode: Int) {
    val intent = Intent()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        intent.putExtra("app_package", activity.packageName)
        intent.putExtra("app_uid", activity.applicationInfo?.uid)
    } else {
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
    }
    activity.startActivityForResult(intent, requestCode)
}

/**
 * Shows notification system settings for the given channel id.
 */
@TargetApi(Build.VERSION_CODES.O)
fun startNotificationChannelSettingsIntent(fragment: Fragment, channelID: String) {
    if (!supportNotificationChannels()) return
    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, fragment.context?.packageName)
        putExtra(Settings.EXTRA_CHANNEL_ID, channelID)
    }
    fragment.startActivity(intent)
}

fun startAddGoogleAccountIntent(context: AppCompatActivity, requestCode: Int) {
    try {
        val intent = Intent(Settings.ACTION_ADD_ACCOUNT)
        intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
        context.startActivityForResult(intent, requestCode)
    } catch (activityNotFoundException: ActivityNotFoundException) {
        context.toast(R.string.error_no_external_application_found)
    }
}

fun startSharePlainTextIntent(fragment: Fragment, chooserTitle: String?, text: String, subject: String? = null) {
    val share = Intent(Intent.ACTION_SEND)
    share.type = "text/plain"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        share.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    } else {
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    // Add data to the intent, the receiving app will decide what to do with it.
    share.putExtra(Intent.EXTRA_SUBJECT, subject)
    share.putExtra(Intent.EXTRA_TEXT, text)
    try {
        fragment.startActivity(Intent.createChooser(share, chooserTitle))
    } catch (activityNotFoundException: ActivityNotFoundException) {
        fragment.activity?.toast(R.string.error_no_external_application_found)
    }
}

fun startImportTextFromFileIntent(fragment: Fragment, requestCode: Int) {
    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "text/plain"
    }
    if (intent.resolveActivity(fragment.activity!!.packageManager) != null) {
        fragment.startActivityForResult(intent, requestCode)
    } else {
        fragment.activity?.toast(R.string.error_no_external_application_found)
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
