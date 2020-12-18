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
import android.os.Build
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutManagerCompat
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class ShortcutsHandler @Inject constructor(
        private val context: Context,
        private val homeRoomListStore: HomeRoomListDataSource,
        private val shortcutCreator: ShortcutCreator
) {

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
                            .map { shortcutCreator.create(it) }

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
}
