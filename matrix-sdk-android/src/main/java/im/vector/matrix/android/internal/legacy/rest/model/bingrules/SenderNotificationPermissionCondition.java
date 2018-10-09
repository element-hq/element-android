/* 
 * Copyright 2014 OpenMarket Ltd
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
package im.vector.matrix.android.internal.legacy.rest.model.bingrules;

import im.vector.matrix.android.internal.legacy.rest.model.PowerLevels;

public class SenderNotificationPermissionCondition extends Condition {
    private static final String LOG_TAG = SenderNotificationPermissionCondition.class.getSimpleName();

    public String key;

    public SenderNotificationPermissionCondition() {
        kind = Condition.KIND_SENDER_NOTIFICATION_PERMISSION;
    }

    public boolean isSatisfied(PowerLevels powerLevels, String userId) {
        return (null != powerLevels) && (null != userId) && powerLevels.getUserPowerLevel(userId) >= powerLevels.notificationLevel(key);
    }

    @Override
    public String toString() {
        return "SenderNotificationPermissionCondition{" + "key=" + key;
    }
}
