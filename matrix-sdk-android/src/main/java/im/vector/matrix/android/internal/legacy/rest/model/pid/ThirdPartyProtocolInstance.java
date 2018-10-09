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

import java.io.Serializable;
import java.util.Map;

/**
 * This class describes a third party protocol instance
 */
public class ThirdPartyProtocolInstance implements Serializable {

    // the network identifier
    public String networkId;

    // the fields (domain...)
    public Map<String, Object> fields;

    // the instance id
    public String instanceId;

    // the description
    public String desc;

    // the dedicated bot
    public String botUserId;

    // the icon URL
    public String icon;
}
