/*
 * Copyright 2015 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.data;

import java.util.Map;

public class Pusher {
    public String pushkey;
    public Object kind;
    public String profileTag;
    public String appId;
    public String appDisplayName;
    public String deviceDisplayName;
    public String lang;
    public Map<String, String> data;
    public Boolean append;


    @Override
    public java.lang.String toString() {
        return "Pusher : \n\tappDisplayName " + appDisplayName + "\n\tdeviceDisplayName " + deviceDisplayName + "\n\tpushkey " + pushkey;
    }
}
