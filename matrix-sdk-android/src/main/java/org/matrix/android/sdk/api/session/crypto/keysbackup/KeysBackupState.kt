/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.keysbackup

/**
 * E2e keys backup states.
 *
 * <pre>
 *                               |
 *                               V        deleteKeyBackupVersion (on current backup)
 *  +---------------------->  UNKNOWN  <-------------
 *  |                            |
 *  |                            | checkAndStartKeysBackup (at startup or on new verified device or a new detected backup)
 *  |                            V
 *  |                     CHECKING BACKUP
 *  |                            |
 *  |    Network error           |
 *  +<----------+----------------+-------> DISABLED <----------------------+
 *  |           |                |            |                            |
 *  |           |                |            | createKeysBackupVersion    |
 *  |           V                |            V                            |
 *  +<---  WRONG VERSION         |         ENABLING                        |
 *      |       ^                |            |                            |
 *      |       |                V       ok   |     error                  |
 *      |       |     +------> READY <--------+----------------------------+
 *      V       |     |          |
 * NOT TRUSTED  |     |          | on new key
 *              |     |          V
 *              |     |     WILL BACK UP (waiting a random duration)
 *              |     |          |
 *              |     |          |
 *              |     | ok       V
 *              |     +----- BACKING UP
 *              |                |
 *              |      Error     |
 *              +<---------------+
 * </pre>
 */
enum class KeysBackupState {
    /**
     * Need to check the current backup version on the homeserver.
     */
    Unknown,

    /**
     * Checking if backup is enabled on homeserver.
     */
    CheckingBackUpOnHomeserver,

    /**
     * Backup has been stopped because a new backup version has been detected on the homeserver.
     */
    WrongBackUpVersion,

    /**
     * Backup from this device is not enabled.
     */
    Disabled,

    /**
     * There is a backup available on the homeserver but it is not trusted.
     * It is not trusted because the signature is invalid or the device that created it is not verified.
     * Use [KeysBackup.getKeysBackupTrust()] to get trust details.
     * Consequently, the backup from this device is not enabled.
     */
    NotTrusted,

    /**
     * Backup is being enabled: the backup version is being created on the homeserver.
     */
    Enabling,

    /**
     * Backup is enabled and ready to send backup to the homeserver.
     */
    ReadyToBackUp,

    /**
     * e2e keys are going to be sent to the homeserver.
     */
    WillBackUp,

    /**
     * e2e keys are being sent to the homeserver.
     */
    BackingUp
}
