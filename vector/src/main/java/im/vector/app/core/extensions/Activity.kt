/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Parcelable
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import im.vector.app.R
import timber.log.Timber

fun ComponentActivity.registerStartForActivityResult(onResult: (ActivityResult) -> Unit): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult(), onResult)
}

fun AppCompatActivity.addFragment(
        container: ViewGroup,
        fragment: Fragment,
        allowStateLoss: Boolean = false
) {
    supportFragmentManager.commitTransaction(allowStateLoss) { add(container.id, fragment) }
}

fun <T : Fragment> AppCompatActivity.addFragment(
        container: ViewGroup,
        fragmentClass: Class<T>,
        params: Parcelable? = null,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    supportFragmentManager.commitTransaction(allowStateLoss) {
        add(container.id, fragmentClass, params.toMvRxBundle(), tag)
    }
}

fun AppCompatActivity.replaceFragment(
        container: ViewGroup,
        fragment: Fragment,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    supportFragmentManager.commitTransaction(allowStateLoss) {
        replace(container.id, fragment, tag)
    }
}

fun <T : Fragment> AppCompatActivity.replaceFragment(
        container: ViewGroup,
        fragmentClass: Class<T>,
        params: Parcelable? = null,
        tag: String? = null,
        allowStateLoss: Boolean = false,
        useCustomAnimation: Boolean = false
) {
    supportFragmentManager.commitTransaction(allowStateLoss) {
        if (useCustomAnimation) {
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
        }
        replace(container.id, fragmentClass, params.toMvRxBundle(), tag)
    }
}

fun AppCompatActivity.addFragmentToBackstack(
        container: ViewGroup,
        fragment: Fragment,
        tag: String? = null,
        allowStateLoss: Boolean = false
) {
    supportFragmentManager.commitTransaction(allowStateLoss) {
        replace(container.id, fragment).addToBackStack(tag)
    }
}

fun <T : Fragment> AppCompatActivity.addFragmentToBackstack(
        container: ViewGroup,
        fragmentClass: Class<T>,
        params: Parcelable? = null,
        tag: String? = null,
        allowStateLoss: Boolean = false,
        option: ((FragmentTransaction) -> Unit)? = null
) {
    supportFragmentManager.commitTransaction(allowStateLoss) {
        option?.invoke(this)
        replace(container.id, fragmentClass, params.toMvRxBundle(), tag).addToBackStack(tag)
    }
}

fun AppCompatActivity.popBackstack() {
    supportFragmentManager.popBackStack()
}

fun AppCompatActivity.resetBackstack() {
    repeat(supportFragmentManager.backStackEntryCount) {
        supportFragmentManager.popBackStack()
    }
}

fun AppCompatActivity.hideKeyboard() {
    currentFocus?.hideKeyboard()
}

/**
 * The current activity must be the root of a task to call onBackPressed, otherwise finish activities with the same task affinity.
 */
fun AppCompatActivity.validateBackPressed(onBackPressed: () -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && supportFragmentManager.backStackEntryCount == 0) {
        if (isTaskRoot) {
            onBackPressed()
        } else {
            Timber.e("Application is potentially corrupted by an unknown activity")
            finishAffinity()
        }
    } else {
        onBackPressed()
    }
}

fun Activity.restart() {
    finish()
    startActivity(intent)
}

fun Activity.keepScreenOn() {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

fun Activity.endKeepScreenOn() {
    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}
