/* 
 * Copyright 2017 Vector Creations Ltd
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
package im.vector.matrix.android.internal.legacy.rest.model.pid;

import java.util.List;
import java.util.Map;

/**
 * This class describes the third party server protocols.
 */
public class ThirdPartyProtocol {
    // the user fields (domain, nick, username...)
    public List<String> userFields;

    // the location fields (domain, channels, room...)
    public List<String> locationFields;

    // the field types
    public Map<String, Map<String, String>> fieldTypes;

    // the protocol instance
    public List<ThirdPartyProtocolInstance> instances;
}
