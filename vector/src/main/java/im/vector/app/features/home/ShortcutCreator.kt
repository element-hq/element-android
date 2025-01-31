/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import android.content.Context
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.MainActivity
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

private val useAdaptiveIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
private const val adaptiveIconSizeDp = 108
private const val adaptiveIconOuterSidesDp = 18

class ShortcutCreator @Inject constructor(
        private val context: Context,
        private val avatarRenderer: AvatarRenderer,
        private val dimensionConverter: DimensionConverter,
        buildMeta: BuildMeta,
) {

    private val directShareCategory = buildMeta.applicationId + ".SHORTCUT_SHARE"
    private val adaptiveIconSize = dimensionConverter.dpToPx(adaptiveIconSizeDp)
    private val adaptiveIconOuterSides = dimensionConverter.dpToPx(adaptiveIconOuterSidesDp)
    private val iconSize by lazy {
        if (useAdaptiveIcon) {
            adaptiveIconSize - (adaptiveIconOuterSides * 2)
        } else {
            dimensionConverter.dpToPx(72)
        }
    }

    fun canCreateShortcut(): Boolean {
        return ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    }

    @WorkerThread
    fun create(roomSummary: RoomSummary, rank: Int = 1): ShortcutInfoCompat {
        val intent = MainActivity.shortcutIntent(context, roomSummary.roomId)
        val bitmap = try {
            val glideRequests = GlideApp.with(context)
            val matrixItem = roomSummary.toMatrixItem()
            when (useAdaptiveIcon) {
                true -> avatarRenderer.adaptiveShortcutDrawable(glideRequests, matrixItem, iconSize, adaptiveIconSize, adaptiveIconOuterSides.toFloat())
                false -> avatarRenderer.shortcutDrawable(glideRequests, matrixItem, iconSize)
            }
        } catch (failure: Throwable) {
            null
        }
        val categories = if (Build.VERSION.SDK_INT >= 25) {
            setOf(directShareCategory, ShortcutInfo.SHORTCUT_CATEGORY_CONVERSATION)
        } else {
            setOf(directShareCategory)
        }

        return ShortcutInfoCompat.Builder(context, roomSummary.roomId)
                .setShortLabel(roomSummary.displayName)
                .setIcon(bitmap?.toProfileImageIcon())
                .setIntent(intent)
                .setLongLived(true)
                .setRank(rank)
                .setCategories(categories)
                .build()
    }

    private fun Bitmap.toProfileImageIcon(): IconCompat {
        return if (useAdaptiveIcon) {
            IconCompat.createWithAdaptiveBitmap(this)
        } else {
            IconCompat.createWithBitmap(this)
        }
    }
}
