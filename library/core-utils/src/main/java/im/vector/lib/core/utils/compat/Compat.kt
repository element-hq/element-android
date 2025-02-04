/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.core.utils.compat

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

inline fun <reified T> Intent.getParcelableExtraCompat(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T?
}

inline fun <reified T : Parcelable> Intent.getParcelableArrayListExtraCompat(key: String): ArrayList<T>? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableArrayListExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayListExtra<T>(key)
}

inline fun <reified T> Bundle.getParcelableCompat(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T?
}

inline fun <reified T : Serializable> Bundle.getSerializableCompat(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T?
}

inline fun <reified T : Serializable> Intent.getSerializableExtraCompat(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializableExtra(key) as? T?
}

fun PackageManager.queryIntentActivitiesCompat(data: Intent, flags: Int): List<ResolveInfo> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> queryIntentActivities(
                data,
                PackageManager.ResolveInfoFlags.of(flags.toLong())
        )
        else -> @Suppress("DEPRECATION") queryIntentActivities(data, flags)
    }
}

fun PackageManager.resolveActivityCompat(data: Intent, flags: Int): ResolveInfo? {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> resolveActivity(
                data,
                PackageManager.ResolveInfoFlags.of(flags.toLong())
        )
        else -> @Suppress("DEPRECATION") resolveActivity(data, flags)
    }
}
