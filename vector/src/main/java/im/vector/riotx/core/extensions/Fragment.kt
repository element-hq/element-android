/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.core.extensions

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Parcelable
import androidx.fragment.app.Fragment
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.core.utils.toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun VectorBaseFragment.addFragment(frameId: Int, fragment: Fragment) {
    parentFragmentManager.commitTransactionNow { add(frameId, fragment) }
}

fun <T : Fragment> VectorBaseFragment.addFragment(frameId: Int, fragmentClass: Class<T>, params: Parcelable? = null, tag: String? = null) {
    parentFragmentManager.commitTransactionNow {
        add(frameId, fragmentClass, params.toMvRxBundle(), tag)
    }
}

fun VectorBaseFragment.replaceFragment(frameId: Int, fragment: Fragment) {
    parentFragmentManager.commitTransactionNow { replace(frameId, fragment) }
}

fun <T : Fragment> VectorBaseFragment.replaceFragment(frameId: Int, fragmentClass: Class<T>, params: Parcelable? = null, tag: String? = null) {
    parentFragmentManager.commitTransactionNow {
        replace(frameId, fragmentClass, params.toMvRxBundle(), tag)
    }
}

fun VectorBaseFragment.addFragmentToBackstack(frameId: Int, fragment: Fragment, tag: String? = null) {
    parentFragmentManager.commitTransaction { replace(frameId, fragment, tag).addToBackStack(tag) }
}

fun <T : Fragment> VectorBaseFragment.addFragmentToBackstack(frameId: Int, fragmentClass: Class<T>, params: Parcelable? = null, tag: String? = null) {
    parentFragmentManager.commitTransaction {
        replace(frameId, fragmentClass, params.toMvRxBundle(), tag).addToBackStack(tag)
    }
}

fun VectorBaseFragment.addChildFragment(frameId: Int, fragment: Fragment, tag: String? = null) {
    childFragmentManager.commitTransactionNow { add(frameId, fragment, tag) }
}

fun <T : Fragment> VectorBaseFragment.addChildFragment(frameId: Int, fragmentClass: Class<T>, params: Parcelable? = null, tag: String? = null) {
    childFragmentManager.commitTransactionNow {
        add(frameId, fragmentClass, params.toMvRxBundle(), tag)
    }
}

fun VectorBaseFragment.replaceChildFragment(frameId: Int, fragment: Fragment, tag: String? = null) {
    childFragmentManager.commitTransactionNow { replace(frameId, fragment, tag) }
}

fun <T : Fragment> VectorBaseFragment.replaceChildFragment(frameId: Int, fragmentClass: Class<T>, params: Parcelable? = null, tag: String? = null) {
    childFragmentManager.commitTransactionNow {
        replace(frameId, fragmentClass, params.toMvRxBundle(), tag)
    }
}

fun VectorBaseFragment.addChildFragmentToBackstack(frameId: Int, fragment: Fragment, tag: String? = null) {
    childFragmentManager.commitTransaction { replace(frameId, fragment).addToBackStack(tag) }
}

fun <T : Fragment> VectorBaseFragment.addChildFragmentToBackstack(frameId: Int, fragmentClass: Class<T>, params: Parcelable? = null, tag: String? = null) {
    childFragmentManager.commitTransaction {
        replace(frameId, fragmentClass, params.toMvRxBundle(), tag).addToBackStack(tag)
    }
}

/**
 * Return a list of all child Fragments, recursively
 */
fun Fragment.getAllChildFragments(): List<Fragment> {
    return listOf(this) + childFragmentManager.fragments.map { it.getAllChildFragments() }.flatten()
}

// Define a missing constant
const val POP_BACK_STACK_EXCLUSIVE = 0

fun Fragment.queryExportKeys(userId: String, requestCode: Int) {
    // We need WRITE_EXTERNAL permission
//    if (checkPermissions(PERMISSIONS_FOR_WRITING_FILES,
//                    this,
//                    PERMISSION_REQUEST_CODE_EXPORT_KEYS,
//                    R.string.permissions_rationale_msg_keys_backup_export)) {
    // WRITE permissions are not needed
    val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).let {
        it.format(Date())
    }
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = "text/plain"
    intent.putExtra(
            Intent.EXTRA_TITLE,
            "riot-megolm-export-$userId-$timestamp.txt"
    )

    try {
        startActivityForResult(Intent.createChooser(intent, getString(R.string.keys_backup_setup_step1_manual_export)), requestCode)
    } catch (activityNotFoundException: ActivityNotFoundException) {
        activity?.toast(R.string.error_no_external_application_found)
    }
//    }
}
