package im.vector.riotredesign.core.extensions

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

fun AppCompatActivity.addFragment(fragment: Fragment, frameId: Int) {
    supportFragmentManager.inTransaction { add(frameId, fragment) }
}

fun AppCompatActivity.replaceFragment(fragment: Fragment, frameId: Int) {
    supportFragmentManager.inTransaction { replace(frameId, fragment) }
}

fun AppCompatActivity.addFragmentToBackstack(fragment: Fragment, frameId: Int, tag: String? = null) {
    supportFragmentManager.inTransaction { replace(frameId, fragment).addToBackStack(tag) }
}