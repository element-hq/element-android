package im.vector.riotredesign.core.utils

import android.os.Binder
import android.os.Bundle
import android.support.v4.app.BundleCompat
import android.support.v4.app.Fragment
import kotlin.reflect.KProperty

class FragmentArgumentDelegate<T : Any> : kotlin.properties.ReadWriteProperty<Fragment, T?> {

    var value: T? = null

    override operator fun getValue(thisRef: android.support.v4.app.Fragment, property: kotlin.reflect.KProperty<*>): T? {
        if (value == null) {
            val args = thisRef.arguments
            @Suppress("UNCHECKED_CAST")
            value = args?.get(property.name) as T?
        }
        return value
    }

    override operator fun setValue(thisRef: Fragment, property: KProperty<*>, value: T?) {
        if (value == null) return

        if (thisRef.arguments == null) {
            thisRef.arguments = Bundle()
        }
        val args = thisRef.arguments!!
        val key = property.name

        when (value) {
            is String                -> args.putString(key, value)
            is Int                   -> args.putInt(key, value)
            is Short                 -> args.putShort(key, value)
            is Long                  -> args.putLong(key, value)
            is Byte                  -> args.putByte(key, value)
            is ByteArray             -> args.putByteArray(key, value)
            is Char                  -> args.putChar(key, value)
            is CharArray             -> args.putCharArray(key, value)
            is CharSequence          -> args.putCharSequence(key, value)
            is Float                 -> args.putFloat(key, value)
            is Bundle                -> args.putBundle(key, value)
            is Binder                -> BundleCompat.putBinder(args, key, value)
            is android.os.Parcelable -> args.putParcelable(key, value)
            is java.io.Serializable  -> args.putSerializable(key, value)
            else                     -> throw IllegalStateException("Type ${value.javaClass.name} of property ${property.name} is not supported")
        }
    }
}

class UnsafeFragmentArgumentDelegate<T : Any> : kotlin.properties.ReadWriteProperty<Fragment, T> {

    private val innerDelegate = FragmentArgumentDelegate<T>()

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) {
        innerDelegate.setValue(thisRef, property, value)
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        return innerDelegate.getValue(thisRef, property)!!
    }

}