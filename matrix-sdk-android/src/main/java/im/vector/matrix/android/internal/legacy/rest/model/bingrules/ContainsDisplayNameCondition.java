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

import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.message.Message;
import im.vector.matrix.android.internal.legacy.util.EventUtils;
import im.vector.matrix.android.internal.legacy.util.JsonUtils;

/**
 * Bing rule condition that is satisfied when a message body contains the user's current display name.
 */
public class ContainsDisplayNameCondition extends Condition {
    public ContainsDisplayNameCondition() {
        kind = Condition.KIND_CONTAINS_DISPLAY_NAME;
    }

    public boolean isSatisfied(Event event, String myDisplayName) {
        if (Event.EVENT_TYPE_MESSAGE.equals(event.getType())) {
            Message msg = JsonUtils.toMessage(event.getContent());

            if (null != msg) {
                return EventUtils.caseInsensitiveFind(myDisplayName, msg.body);
            }
        }
        return false;
    }
}
