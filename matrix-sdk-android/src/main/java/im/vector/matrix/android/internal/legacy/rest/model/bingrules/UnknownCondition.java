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

public class UnknownCondition extends Condition {
    public UnknownCondition() {
        kind = Condition.KIND_UNKNOWN;
    }

    // unknown conditions: we previously matched all unknown conditions,
    // but given that rules can be added to the base rules on a server,
    // it's probably better to not match unknown conditions.
    public boolean isSatisfied(Event event) {
        return false;
    }

    @Override
    public String toString() {
        return "UnknownCondition";
    }
}
