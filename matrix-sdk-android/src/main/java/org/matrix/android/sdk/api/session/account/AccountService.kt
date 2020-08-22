/*
 * Copyright (c) 2020 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.account

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.util.Cancelable

/**
 * This interface defines methods to manage the account. It's implemented at the session level.
 */
interface AccountService {
    /**
     * Ask the homeserver to change the password.
     * @param password Current password.
     * @param newPassword New password
     */
    fun changePassword(password: String, newPassword: String, callback: MatrixCallback<Unit>): Cancelable

    /**
     * Deactivate the account.
     *
     * This will make your account permanently unusable. You will not be able to log in, and no one will be able to re-register
     * the same user ID. This will cause your account to leave all rooms it is participating in, and it will remove your account
     * details from your identity server. <b>This action is irreversible</b>.\n\nDeactivating your account <b>does not by default
     * cause us to forget messages you have sent</b>. If you would like us to forget your messages, please tick the box below.
     *
     * Message visibility in Matrix is similar to email. Our forgetting your messages means that messages you have sent will not
     * be shared with any new or unregistered users, but registered users who already have access to these messages will still
     * have access to their copy.
     *
     * @param password the account password
     * @param eraseAllData set to true to forget all messages that have been sent. Warning: this will cause future users to see
     * an incomplete view of conversations
     */
    fun deactivateAccount(password: String, eraseAllData: Boolean, callback: MatrixCallback<Unit>): Cancelable
}
