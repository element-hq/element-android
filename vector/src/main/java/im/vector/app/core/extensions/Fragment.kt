/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import im.vector.app.core.utils.selectTxtFileToWrite
import im.vector.lib.strings.CommonStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Fragment.registerStartForActivityResult(onResult: (ActivityResult) -> Unit): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult(), onResult)
}

fun Fragment.addFragment(
        frameId: Int,
        fragment: Fragment,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    parentFragmentManager.commitTransaction(allowStateLoss) { add(frameId, fragment, tag) }
}

fun <T : Fragment> Fragment.addFragment(
        frameId: Int,
        fragmentClass: Class<T>,
        params: Parcelable? = null,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    parentFragmentManager.commitTransaction(allowStateLoss) {
        add(frameId, fragmentClass, params.toMvRxBundle(), tag)
    }
}

fun Fragment.replaceFragment(
        frameId: Int,
        fragment: Fragment,
        allowStateLoss: Boolean = false
) {
    parentFragmentManager.commitTransaction(allowStateLoss) { replace(frameId, fragment) }
}

fun <T : Fragment> Fragment.replaceFragment(
        frameId: Int,
        fragmentClass: Class<T>,
        params: Parcelable? = null,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    parentFragmentManager.commitTransaction(allowStateLoss) {
        replace(frameId, fragmentClass, params.toMvRxBundle(), tag)
    }
}

fun Fragment.addFragmentToBackstack(
        frameId: Int,
        fragment: Fragment,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    parentFragmentManager.commitTransaction(allowStateLoss) { replace(frameId, fragment, tag).addToBackStack(tag) }
}

fun <T : Fragment> Fragment.addFragmentToBackstack(
        frameId: Int,
        fragmentClass: Class<T>,
        params: Parcelable? = null,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    parentFragmentManager.commitTransaction(allowStateLoss) {
        replace(frameId, fragmentClass, params.toMvRxBundle(), tag).addToBackStack(tag)
    }
}

fun Fragment.addChildFragment(
        frameId: Int,
        fragment: Fragment,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    childFragmentManager.commitTransaction(allowStateLoss) { add(frameId, fragment, tag) }
}

fun <T : Fragment> Fragment.addChildFragment(
        frameId: Int,
        fragmentClass: Class<T>,
        params: Parcelable? = null,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    childFragmentManager.commitTransaction(allowStateLoss) {
        add(frameId, fragmentClass, params.toMvRxBundle(), tag)
    }
}

fun Fragment.replaceChildFragment(
        frameId: Int,
        fragment: Fragment,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    childFragmentManager.commitTransaction(allowStateLoss) { replace(frameId, fragment, tag) }
}

fun <T : Fragment> Fragment.replaceChildFragment(
        frameId: Int,
        fragmentClass: Class<T>,
        params: Parcelable? = null,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    childFragmentManager.commitTransaction(allowStateLoss) {
        replace(frameId, fragmentClass, params.toMvRxBundle(), tag)
    }
}

fun Fragment.addChildFragmentToBackstack(
        frameId: Int,
        fragment: Fragment,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    childFragmentManager.commitTransaction(allowStateLoss) { replace(frameId, fragment).addToBackStack(tag) }
}

fun <T : Fragment> Fragment.addChildFragmentToBackstack(
        frameId: Int,
        fragmentClass: Class<T>,
        params: Parcelable? = null,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    childFragmentManager.commitTransaction(allowStateLoss) {
        replace(frameId, fragmentClass, params.toMvRxBundle(), tag).addToBackStack(tag)
    }
}

/**
 * Return a list of all child Fragments, recursively.
 */
fun Fragment.getAllChildFragments(): List<Fragment> {
    return listOf(this) + childFragmentManager.fragments.map { it.getAllChildFragments() }.flatten()
}

// Define a missing constant
const val POP_BACK_STACK_EXCLUSIVE = 0

fun Fragment.queryExportKeys(
        userId: String,
        applicationName: String,
        activityResultLauncher: ActivityResultLauncher<Intent>,
) {
    val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val appName = applicationName.replace(" ", "-")

    selectTxtFileToWrite(
            activity = requireActivity(),
            activityResultLauncher = activityResultLauncher,
            defaultFileName = "$appName-megolm-export-$userId-${timestamp}.txt",
            chooserHint = getString(CommonStrings.keys_backup_setup_step1_manual_export)
    )
}

fun Activity.queryExportKeys(
        userId: String,
        applicationName: String,
        activityResultLauncher: ActivityResultLauncher<Intent>,
) {
    val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val appName = applicationName.replace(" ", "-")

    selectTxtFileToWrite(
            activity = this,
            activityResultLauncher = activityResultLauncher,
            defaultFileName = "$appName-megolm-export-$userId-${timestamp}.txt",
            chooserHint = getString(CommonStrings.keys_backup_setup_step1_manual_export)
    )
}
