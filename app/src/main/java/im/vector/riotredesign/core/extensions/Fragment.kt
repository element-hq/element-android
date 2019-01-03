package im.vector.riotredesign.core.extensions

import android.support.v4.app.Fragment

fun Fragment.addFragment(fragment: Fragment, frameId: Int) {
    fragmentManager?.inTransaction { add(frameId, fragment) }
}

fun Fragment.replaceFragment(fragment: Fragment, frameId: Int) {
    fragmentManager?.inTransaction { replace(frameId, fragment) }
}

fun Fragment.addFragmentToBackstack(fragment: Fragment, frameId: Int, tag: String? = null) {
    fragmentManager?.inTransaction { replace(frameId, fragment).addToBackStack(tag) }
}

fun Fragment.addChildFragment(fragment: Fragment, frameId: Int) {
    childFragmentManager.inTransaction { add(frameId, fragment) }
}

fun Fragment.replaceChildFragment(fragment: Fragment, frameId: Int) {
    childFragmentManager.inTransaction { replace(frameId, fragment) }
}

fun Fragment.addChildFragmentToBackstack(fragment: Fragment, frameId: Int, tag: String? = null) {
    childFragmentManager.inTransaction { replace(frameId, fragment).addToBackStack(tag) }
}