package im.vector.riotredesign.core.extensions

import androidx.fragment.app.Fragment

fun androidx.fragment.app.Fragment.addFragment(fragment: Fragment, frameId: Int) {
    fragmentManager?.inTransaction { add(frameId, fragment) }
}

fun androidx.fragment.app.Fragment.replaceFragment(fragment: Fragment, frameId: Int) {
    fragmentManager?.inTransaction { replace(frameId, fragment) }
}

fun androidx.fragment.app.Fragment.addFragmentToBackstack(fragment: Fragment, frameId: Int, tag: String? = null) {
    fragmentManager?.inTransaction { replace(frameId, fragment).addToBackStack(tag) }
}

fun androidx.fragment.app.Fragment.addChildFragment(fragment: Fragment, frameId: Int) {
    childFragmentManager.inTransaction { add(frameId, fragment) }
}

fun androidx.fragment.app.Fragment.replaceChildFragment(fragment: Fragment, frameId: Int) {
    childFragmentManager.inTransaction { replace(frameId, fragment) }
}

fun androidx.fragment.app.Fragment.addChildFragmentToBackstack(fragment: Fragment, frameId: Int, tag: String? = null) {
    childFragmentManager.inTransaction { replace(frameId, fragment).addToBackStack(tag) }
}