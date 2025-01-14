/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.telecom;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.jitsi.meet.sdk.ConnectionService;

@RequiresApi(api = Build.VERSION_CODES.O)
public class CallConnectionService extends ConnectionService {
}
