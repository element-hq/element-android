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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.hilt.EntryPoints
import im.vector.app.core.datastore.dataStoreProvider
import im.vector.app.core.di.SingletonEntryPoint
import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider
import java.io.OutputStream
import kotlin.math.roundToInt

fun Context.singletonEntryPoint(): SingletonEntryPoint {
    return EntryPoints.get(applicationContext, SingletonEntryPoint::class.java)
}

fun Context.getDrawableAsSpannable(@DrawableRes drawableRes: Int, alignment: Int = ImageSpan.ALIGN_BOTTOM): Spannable {
    return SpannableString(" ").apply {
        val span = ContextCompat.getDrawable(this@getDrawableAsSpannable, drawableRes)?.let {
            it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
            ImageSpan(it, alignment)
        }
        setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    }
}

fun Context.getResTintedDrawable(@DrawableRes drawableRes: Int, @ColorRes tint: Int, @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1f): Drawable? {
    return getTintedDrawable(drawableRes, ContextCompat.getColor(this, tint), alpha)
}

fun Context.getTintedDrawable(
        @DrawableRes drawableRes: Int,
        @ColorInt tint: Int,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1f
) = ContextCompat.getDrawable(this, drawableRes)
        ?.mutate()
        ?.also { drawable ->
            drawable.setTint(tint)
            alpha.let {
                drawable.alpha = it.toAndroidAlpha()
            }
        }

private fun Float.toAndroidAlpha(): Int {
    return (this * 255).roundToInt()
}

val Context.dataStoreProvider: (String) -> DataStore<Preferences> by dataStoreProvider()

/**
 * Open Uri in truncate mode to make sure we don't partially overwrite content when we get passed a Uri to an existing file.
 */
fun Context.safeOpenOutputStream(uri: Uri): OutputStream? {
    return contentResolver.openOutputStream(uri, "wt")
}

/**
 * Checks for an active connection to infer if the device is offline.
 * This is useful for breaking down UnknownHost exceptions and should not be used to determine if a valid connection is present
 *
 * @return true if no active connection is found
 */
@Suppress("deprecation")
@SuppressLint("NewApi") // false positive
fun Context.inferNoConnectivity(sdkIntProvider: BuildVersionSdkIntProvider): Boolean {
    val connectivityManager = getSystemService<ConnectivityManager>()!!
    return if (sdkIntProvider.get() > Build.VERSION_CODES.M) {
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        when {
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> false
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> false
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> false
            else -> true
        }
    } else {
        when (connectivityManager.activeNetworkInfo?.type) {
            ConnectivityManager.TYPE_WIFI -> false
            ConnectivityManager.TYPE_MOBILE -> false
            ConnectivityManager.TYPE_VPN -> false
            else -> true
        }
    }
}
