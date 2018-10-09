/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.internal.legacy.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Model for https://matrix.org/docs/spec/client_server/r0.3.0.html#get-matrix-client-versions
 * <p>
 * Ex: {"unstable_features": {"m.lazy_load_members": true}, "versions": ["r0.0.1", "r0.1.0", "r0.2.0", "r0.3.0"]}
 */
public class Versions {

    @SerializedName("versions")
    public List<String> supportedVersions;

    @SerializedName("unstable_features")
    public Map<String, Boolean> unstableFeatures;
}
