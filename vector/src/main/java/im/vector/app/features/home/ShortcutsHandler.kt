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
import im.vector.app.core.di.ActiveSessionHolder
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import org.matrix.android.sdk.api.query.RoomTagQueryFilter
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.rx.asObservable
import javax.inject.Inject

class ShortcutsHandler @Inject constructor(
        private val context: Context,
        private val shortcutCreator: ShortcutCreator,
        private val activeSessionHolder: ActiveSessionHolder
) {

    fun observeRoomsAndBuildShortcuts(): Disposable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            // No op
            return Disposables.empty()
        }

        return activeSessionHolder.getSafeActiveSession()
                ?.getPagedRoomSummariesLive(
                        roomSummaryQueryParams {
                            memberships = listOf(Membership.JOIN)
                            roomTagQueryFilter = RoomTagQueryFilter(isFavorite = true, null, null)
                        }
                )
                ?.asObservable()
                ?.subscribe { rooms ->
                    val shortcuts = rooms
                            .take(n = 4) // Android only allows us to create 4 shortcuts
                            .map { shortcutCreator.create(it) }

                    ShortcutManagerCompat.removeAllDynamicShortcuts(context)
                    ShortcutManagerCompat.addDynamicShortcuts(context, shortcuts)
                }
                ?: Disposables.empty()
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
