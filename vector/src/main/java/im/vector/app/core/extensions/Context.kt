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

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.hilt.EntryPoints
import im.vector.app.core.datastore.dataStoreProvider
import im.vector.app.core.di.SingletonEntryPoint
import kotlin.math.roundToInt

fun Context.singletonEntryPoint(): SingletonEntryPoint {
    return EntryPoints.get(applicationContext, SingletonEntryPoint::class.java)
}

fun Context.getResTintedDrawable(@DrawableRes drawableRes: Int, @ColorRes tint: Int, @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1f): Drawable? {
    return getTintedDrawable(drawableRes, ContextCompat.getColor(this, tint), alpha)
}

fun Context.getTintedDrawable(@DrawableRes drawableRes: Int,
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
