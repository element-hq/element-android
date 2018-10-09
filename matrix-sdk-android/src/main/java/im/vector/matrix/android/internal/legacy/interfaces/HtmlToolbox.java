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

package im.vector.matrix.android.internal.legacy.interfaces;

import android.support.annotation.Nullable;
import android.text.Html;

public interface HtmlToolbox {

    /**
     * Convert a html String
     * Example: remove not supported html tags, etc.
     *
     * @param html the source HTML
     * @return the converted HTML
     */
    String convert(String html);

    /**
     * Get a HTML Image Getter
     *
     * @return a HTML Image Getter or null
     */
    @Nullable
    Html.ImageGetter getImageGetter();

    /**
     * Get a HTML Tag Handler
     *
     * @param html the source HTML
     * @return a HTML Tag Handler or null
     */
    @Nullable
    Html.TagHandler getTagHandler(String html);
}
