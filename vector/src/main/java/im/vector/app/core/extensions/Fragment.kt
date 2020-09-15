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

package im.vector.app.core.extensions

import android.app.Activity
import android.os.Parcelable
import androidx.fragment.app.Fragment
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.selectTxtFileToWrite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun VectorBaseFragment.addFragment(
        frameId: Int,
        fragment: Fragment,
        allowStateLoss: Boolean = false
) {
    parentFragmentManager.commitTransaction(allowStateLoss) { add(frameId, fragment) }
}

fun <T : Fragment> VectorBaseFragment.addFragment(
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

fun VectorBaseFragment.replaceFragment(
        frameId: Int,
        fragment: Fragment,
        allowStateLoss: Boolean = false
) {
    parentFragmentManager.commitTransaction(allowStateLoss) { replace(frameId, fragment) }
}

fun <T : Fragment> VectorBaseFragment.replaceFragment(
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

fun VectorBaseFragment.addFragmentToBackstack(
        frameId: Int,
        fragment: Fragment,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    parentFragmentManager.commitTransaction(allowStateLoss) { replace(frameId, fragment, tag).addToBackStack(tag) }
}

fun <T : Fragment> VectorBaseFragment.addFragmentToBackstack(
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

fun VectorBaseFragment.addChildFragment(
        frameId: Int,
        fragment: Fragment,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    childFragmentManager.commitTransaction(allowStateLoss) { add(frameId, fragment, tag) }
}

fun <T : Fragment> VectorBaseFragment.addChildFragment(
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

fun VectorBaseFragment.replaceChildFragment(
        frameId: Int,
        fragment: Fragment,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    childFragmentManager.commitTransaction(allowStateLoss) { replace(frameId, fragment, tag) }
}

fun <T : Fragment> VectorBaseFragment.replaceChildFragment(
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

fun VectorBaseFragment.addChildFragmentToBackstack(
        frameId: Int,
        fragment: Fragment,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    childFragmentManager.commitTransaction(allowStateLoss) { replace(frameId, fragment).addToBackStack(tag) }
}

fun <T : Fragment> VectorBaseFragment.addChildFragmentToBackstack(
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
 * Return a list of all child Fragments, recursively
 */
fun Fragment.getAllChildFragments(): List<Fragment> {
    return listOf(this) + childFragmentManager.fragments.map { it.getAllChildFragments() }.flatten()
}

// Define a missing constant
const val POP_BACK_STACK_EXCLUSIVE = 0

fun Fragment.queryExportKeys(userId: String, requestCode: Int) {
    val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    selectTxtFileToWrite(
            activity = requireActivity(),
            fragment = this,
            defaultFileName = "element-megolm-export-$userId-$timestamp.txt",
            chooserHint = getString(R.string.keys_backup_setup_step1_manual_export),
            requestCode = requestCode
    )
}

fun Activity.queryExportKeys(userId: String, requestCode: Int) {
    val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    selectTxtFileToWrite(
            activity = this,
            fragment = null,
            defaultFileName = "element-megolm-export-$userId-$timestamp.txt",
            chooserHint = getString(R.string.keys_backup_setup_step1_manual_export),
            requestCode = requestCode
    )
}
