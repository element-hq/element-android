/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.helper

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.VisibleForTesting
import im.vector.app.R
import im.vector.app.core.resources.ColorProvider
import org.matrix.android.sdk.api.util.MatrixItem
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class MatrixItemColorProvider @Inject constructor(
        private val colorProvider: ColorProvider
) {
    private val cache = mutableMapOf<String, Int>()

    @ColorInt
    fun getColor(matrixItem: MatrixItem): Int {
        return cache.getOrPut(matrixItem.id) {
            when (matrixItem) {
                is MatrixItem.UserItem -> getColorFromUserIdColorInt(colorProvider, matrixItem.id)
                else                   -> getColorFromRoomId(colorProvider, matrixItem.id)
            }
        }
    }

    fun setOverrideColors(overrideColors: Map<String, String>?) {
        cache.clear()
        overrideColors?.forEach {
            setOverrideColor(it.key, it.value)
        }
    }

    fun setOverrideColor(id: String, colorSpec: String?): Boolean {
        val color = parseUserColorSpec(colorSpec)
        return if (color == null) {
            cache.remove(id)
            false
        } else {
            cache[id] = color
            true
        }
    }

    @ColorInt
    private fun parseUserColorSpec(colorText: String?): Int? {
        return if (colorText.isNullOrBlank()) {
            null
        } else {
            try {
                if (colorText.length == 1) {
                    colorProvider.getColor(getUserColorByIndex(colorText.toInt()))
                } else {
                    Color.parseColor(colorText)
                }
            } catch (e: Throwable) {
                Timber.e(e, "Unable to parse color $colorText")
                null
            }
        }
    }

    companion object {
        @ColorRes
        @VisibleForTesting
        fun getColorFromUserId(userId: String?): Int {
            var hash = 0

            userId?.toList()?.map { chr -> hash = (hash shl 5) - hash + chr.code }

            return getUserColorByIndex(abs(hash))
        }

        @ColorInt
        @VisibleForTesting
        fun getColorFromUserIdColorInt(colorProvider: ColorProvider, userId: String?): Int {
            var hash = 0

            userId?.toList()?.map { chr -> hash = (hash shl 5) - hash + chr.code }

            return getUserColorByIndexColorInt(colorProvider, abs(hash))
        }

        @ColorRes
        private fun getUserColorByIndex(index: Int): Int {
            return when (index % 8) {
                1    -> R.color.element_name_02
                2    -> R.color.element_name_03
                3    -> R.color.element_name_04
                4    -> R.color.element_name_05
                5    -> R.color.element_name_06
                6    -> R.color.element_name_07
                7    -> R.color.element_name_08
                else -> R.color.element_name_01
            }
        }

        
        @ColorInt
        private fun getUserColorByIndexColorInt(colorProvider: ColorProvider, index: Int): Int {
            return when (index % 8) {
                1    -> colorProvider.getColorFromAttribute(R.attr.vctr_element_name_02)
                2    -> colorProvider.getColorFromAttribute(R.attr.vctr_element_name_03)
                3    -> colorProvider.getColorFromAttribute(R.attr.vctr_element_name_04)
                4    -> colorProvider.getColorFromAttribute(R.attr.vctr_element_name_05)
                5    -> colorProvider.getColorFromAttribute(R.attr.vctr_element_name_06)
                6    -> colorProvider.getColorFromAttribute(R.attr.vctr_element_name_07)
                7    -> colorProvider.getColorFromAttribute(R.attr.vctr_element_name_08)
                else -> colorProvider.getColorFromAttribute(R.attr.vctr_element_name_01)
            }
        }


        @ColorInt
        private fun getColorFromRoomId(roomId: String?): Int {
            return when ((roomId?.toList()?.sumOf { it.code } ?: 0) % 3) {
                1    -> colorProvider.getColorFromAttribute(R.attr.vctr_element_room_02)
                2    -> colorProvider.getColorFromAttribute(R.attr.vctr_element_room_03)
                else -> colorProvider.getColorFromAttribute(R.attr.vctr_element_room_01)
            }
        }
    }
}
