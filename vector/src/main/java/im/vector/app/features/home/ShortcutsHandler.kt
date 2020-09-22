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

package im.vector.app.features.home

import android.content.Context
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.room.detail.RoomDetailActivity
import org.matrix.android.sdk.api.util.toMatrixItem
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

private val useAdaptiveIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
private const val adaptiveIconSizeDp = 108
private const val adaptiveIconOuterSidesDp = 18

class ShortcutsHandler @Inject constructor(
        private val context: Context,
        private val homeRoomListStore: HomeRoomListDataSource,
        private val avatarRenderer: AvatarRenderer,
        private val dimensionConverter: DimensionConverter
) {
    private val adaptiveIconSize = dimensionConverter.dpToPx(adaptiveIconSizeDp)
    private val adaptiveIconOuterSides = dimensionConverter.dpToPx(adaptiveIconOuterSidesDp)
    private val iconSize by lazy {
        if (useAdaptiveIcon) {
            adaptiveIconSize - adaptiveIconOuterSides
        } else {
            dimensionConverter.dpToPx(72)
        }
    }

    fun observeRoomsAndBuildShortcuts(): Disposable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            // No op
            return Observable.empty<Unit>().subscribe()
        }

        return homeRoomListStore
                .observe()
                .distinctUntilChanged()
                .observeOn(Schedulers.computation())
                .subscribe { rooms ->
                    val shortcuts = rooms
                            .filter { room -> room.isFavorite }
                            .take(n = 4) // Android only allows us to create 4 shortcuts
                            .map { room ->
                                val intent = RoomDetailActivity.shortcutIntent(context, room.roomId)
                                val bitmap = try {
                                    avatarRenderer.shortcutDrawable(GlideApp.with(context), room.toMatrixItem(), iconSize)
                                } catch (failure: Throwable) {
                                    null
                                }
                                ShortcutInfoCompat.Builder(context, room.roomId)
                                        .setShortLabel(room.displayName)
                                        .setIcon(bitmap?.toProfileImageIcon())
                                        .setIntent(intent)
                                        .build()
                            }

                    ShortcutManagerCompat.removeAllDynamicShortcuts(context)
                    ShortcutManagerCompat.addDynamicShortcuts(context, shortcuts)
                }
    }

    fun clearShortcuts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            // No op
            return
        }

        ShortcutManagerCompat.removeAllDynamicShortcuts(context)

        // We can only disabled pinned shortcuts with the API, but at least it will prevent the crash
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                context.getSystemService<ShortcutManager>()
                        ?.let {
                            it.disableShortcuts(it.pinnedShortcuts.map { pinnedShortcut -> pinnedShortcut.id })
                        }
            }
        }
    }

    // PRIVATE API *********************************************************************************

    private fun Bitmap.toProfileImageIcon(): IconCompat {
        return if (useAdaptiveIcon) {
            IconCompat.createWithAdaptiveBitmap(this)
        } else {
            IconCompat.createWithBitmap(this)
        }
    }
}
