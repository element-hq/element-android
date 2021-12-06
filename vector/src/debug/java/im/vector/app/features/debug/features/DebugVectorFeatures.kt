/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.debug.features

import android.content.Context
import android.content.SharedPreferences
import im.vector.app.features.DefaultVectorFeatures
import im.vector.app.features.VectorFeatures
import kotlin.reflect.KClass

class DebugVectorFeatures(
        context: Context,
        private val vectorFeatures: DefaultVectorFeatures
) : VectorFeatures {

    private val featurePrefs = context.getSharedPreferences("debug-features", Context.MODE_PRIVATE)

    override fun loginVersion(): VectorFeatures.LoginVersion {
        return featurePrefs.readEnum<VectorFeatures.LoginVersion>() ?: vectorFeatures.loginVersion()
    }

    fun <T : Enum<T>> hasEnumOverride(type: KClass<T>): Boolean {
        return featurePrefs.containsEnum(type)
    }

    fun <T : Enum<T>> overrideEnum(value: T?, type: KClass<T>) {
        if (value == null) {
            featurePrefs.removeEnum(type)
        } else {
            featurePrefs.putEnum(value, type)
        }
    }
}

private fun <T : Enum<T>> SharedPreferences.removeEnum(type: KClass<T>) {
    edit().remove("enum-${type.simpleName}").apply()
}

private fun <T : Enum<T>> SharedPreferences.containsEnum(type: KClass<T>): Boolean {
    return contains("enum-${type.simpleName}")
}

private inline fun <reified T : Enum<T>> SharedPreferences.readEnum(): T? {
    val value = T::class.simpleName
    return getString("enum-$value", null)?.let { enumValueOf<T>(it) }
}

private fun <T : Enum<T>> SharedPreferences.putEnum(value: T, type: KClass<T>) {
    edit()
            .putString("enum-${type.simpleName}", value.name)
            .apply()
}
