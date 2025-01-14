/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.DefaultPreferences
import im.vector.app.core.dispatchers.CoroutineDispatchers
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.MainActivity
import im.vector.app.features.home.room.detail.RoomDetailActivity
import im.vector.app.features.pin.PinCodeStore
import im.vector.app.features.pin.PinCodeStoreListener
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.flow.flow
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class ShortcutsHandler @Inject constructor(
        private val context: Context,
        private val stringProvider: StringProvider,
        private val appDispatchers: CoroutineDispatchers,
        private val shortcutCreator: ShortcutCreator,
        private val activeSessionHolder: ActiveSessionHolder,
        private val pinCodeStore: PinCodeStore,
        @DefaultPreferences
        private val sharedPreferences: SharedPreferences,
) : PinCodeStoreListener {

    private val isRequestPinShortcutSupported = ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    private val maxShortcutCountPerActivity = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)

    // Value will be set correctly if necessary
    private var hasPinCode = AtomicBoolean(true)

    fun observeRoomsAndBuildShortcuts(coroutineScope: CoroutineScope): Job {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            // No op
            return Job()
        }
        coroutineScope.launch {
            hasPinCode.set(pinCodeStore.hasEncodedPin())
        }
        val session = activeSessionHolder.getSafeActiveSession() ?: return Job()
        return session.flow().liveRoomSummaries(
                roomSummaryQueryParams {
                    memberships = listOf(Membership.JOIN)
                },
                sortOrder = RoomSortOrder.PRIORITY_AND_ACTIVITY
        )
                .onStart { pinCodeStore.addListener(this@ShortcutsHandler) }
                .onCompletion { pinCodeStore.removeListener(this@ShortcutsHandler) }
                .onEach { rooms ->
                    // Remove dead shortcuts (i.e. deleted rooms)
                    removeDeadShortcuts(rooms.map { it.roomId })

                    // Create shortcuts
                    createShortcuts(rooms)
                }
                .flowOn(appDispatchers.computation)
                .launchIn(coroutineScope)
    }

    @SuppressLint("RestrictedApi")
    fun updateShortcutsWithPreviousIntent() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        // Check if it's been already done
        if (sharedPreferences.getBoolean(SHARED_PREF_KEY, false)) return
        ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
                .filter { it.intent.component?.className == RoomDetailActivity::class.qualifiedName }
                .mapNotNull {
                    it.intent.getStringExtra("EXTRA_ROOM_ID")?.let { roomId ->
                        ShortcutInfoCompat.Builder(context, it.toShortcutInfo())
                                .setIntent(MainActivity.shortcutIntent(context, roomId))
                                .build()
                    }
                }
                .takeIf { it.isNotEmpty() }
                ?.also { Timber.d("Update ${it.size} shortcut(s)") }
                ?.let { tryOrNull("Error") { ShortcutManagerCompat.updateShortcuts(context, it) } }
                ?.also { Timber.d("Update shortcuts with success: $it") }
        sharedPreferences.edit { putBoolean(SHARED_PREF_KEY, true) }
    }

    private fun removeDeadShortcuts(roomIds: List<String>) {
        val deadShortcutIds = ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_DYNAMIC)
                .map { it.id }
                .filter { !roomIds.contains(it) }

        if (deadShortcutIds.isNotEmpty()) {
            Timber.d("Removing shortcut(s) $deadShortcutIds")
            ShortcutManagerCompat.removeLongLivedShortcuts(context, deadShortcutIds)
            if (isRequestPinShortcutSupported) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    ShortcutManagerCompat.disableShortcuts(
                            context,
                            deadShortcutIds,
                            stringProvider.getString(CommonStrings.shortcut_disabled_reason_room_left)
                    )
                }
            }
        }
    }

    private fun createShortcuts(rooms: List<RoomSummary>) {
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)

        // No shortcut in this case (privacy)
        if (hasPinCode.get()) return

        val shortcuts = rooms
                .take(maxShortcutCountPerActivity)
                .mapIndexed { index, room ->
                    shortcutCreator.create(room, index)
                }

        shortcuts.forEach { shortcut ->
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        }
    }

    fun clearShortcuts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            // No op
            return
        }

        // according to Android documentation
        // removeLongLivedShortcuts for API 29 and lower should behave like removeDynamicShortcuts(Context, List)
        // getDynamicShortcuts: returns all dynamic shortcuts from the app.
        val shortcuts = ShortcutManagerCompat.getDynamicShortcuts(context).map { it.id }
        ShortcutManagerCompat.removeLongLivedShortcuts(context, shortcuts)

        // We can only disabled pinned shortcuts with the API, but at least it will prevent the crash
        if (isRequestPinShortcutSupported) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                context.getSystemService<ShortcutManager>()
                        ?.pinnedShortcuts
                        ?.takeIf { it.isNotEmpty() }
                        ?.map { pinnedShortcut -> pinnedShortcut.id }
                        ?.let { shortcutIdsToDisable ->
                            ShortcutManagerCompat.disableShortcuts(
                                    context,
                                    shortcutIdsToDisable,
                                    stringProvider.getString(CommonStrings.shortcut_disabled_reason_sign_out)
                            )
                        }
            }
        }
    }

    override fun onPinSetUpChange(isConfigured: Boolean) {
        hasPinCode.set(isConfigured)
        if (isConfigured) {
            // Remove shortcuts immediately
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
        }
        // Else shortcut will be created next time any room summary is updated, or
        // next time the app is started which is acceptable
    }

    companion object {
        const val SHARED_PREF_KEY = "ROOM_DETAIL_ACTIVITY_SHORTCUT_UPDATED"
    }
}
