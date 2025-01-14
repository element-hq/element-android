/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.upgrade

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized

data class MigrateRoomViewState(
        val roomId: String,
        val newVersion: String,
        val customDescription: CharSequence? = null,
        val currentVersion: String? = null,
        val isPublic: Boolean = false,
        val shouldIssueInvites: Boolean = false,
        val shouldUpdateKnownParents: Boolean = true,
        val otherMemberCount: Int = 0,
        val knownParents: List<String> = emptyList(),
        val upgradingStatus: Async<UpgradeRoomViewModelTask.Result> = Uninitialized,
        val upgradingProgress: Int = 0,
        val upgradingProgressTotal: Int = 0,
        val upgradingProgressIndeterminate: Boolean = true,
        val migrationReason: MigrateRoomBottomSheet.MigrationReason = MigrateRoomBottomSheet.MigrationReason.MANUAL,
        val autoMigrateMembersAndParents: Boolean = false
) : MavericksState {
    constructor(args: MigrateRoomBottomSheet.Args) : this(
            roomId = args.roomId,
            newVersion = args.newVersion,
            migrationReason = args.reason,
            autoMigrateMembersAndParents = args.reason == MigrateRoomBottomSheet.MigrationReason.FOR_RESTRICTED,
            customDescription = args.customDescription
    )
}
