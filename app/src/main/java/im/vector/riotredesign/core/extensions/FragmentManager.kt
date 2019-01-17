package im.vector.riotredesign.core.extensions

import androidx.fragment.app.FragmentTransaction

inline fun androidx.fragment.app.FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
    beginTransaction().func().commit()
}