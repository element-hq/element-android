/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.html;

import androidx.annotation.NonNull;

import org.commonmark.node.ListItem;

import java.util.Arrays;
import java.util.Collection;

import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.MarkwonVisitor;
import io.noties.markwon.RenderProps;
import io.noties.markwon.SpanFactory;
import io.noties.markwon.SpannableBuilder;
import io.noties.markwon.core.CoreProps;
import io.noties.markwon.html.HtmlTag;
import io.noties.markwon.html.MarkwonHtmlRenderer;
import io.noties.markwon.html.TagHandler;

/**
 * Copied from https://github.com/noties/Markwon/blob/master/markwon-html/src/main/java/io/noties/markwon/html/tag/ListHandler.java#L44
 * With a modification on the starting list position
 */
public class ListHandlerWithInitialStart extends TagHandler {

    private static final String START_KEY = "start";

    @Override
    public void handle(
            @NonNull MarkwonVisitor visitor,
            @NonNull MarkwonHtmlRenderer renderer,
            @NonNull HtmlTag tag) {

        if (!tag.isBlock()) {
            return;
        }

        final HtmlTag.Block block = tag.getAsBlock();
        final boolean ol = "ol".equals(block.name());
        final boolean ul = "ul".equals(block.name());

        if (!ol && !ul) {
            return;
        }

        final MarkwonConfiguration configuration = visitor.configuration();
        final RenderProps renderProps = visitor.renderProps();
        final SpanFactory spanFactory = configuration.spansFactory().get(ListItem.class);

        // Modified line
        int number = Integer.parseInt(block.attributes().containsKey(START_KEY) ? block.attributes().get(START_KEY) : "1");

        final int bulletLevel = currentBulletListLevel(block);

        for (HtmlTag.Block child : block.children()) {

            visitChildren(visitor, renderer, child);

            if (spanFactory != null && "li".equals(child.name())) {

                // insert list item here
                if (ol) {
                    CoreProps.LIST_ITEM_TYPE.set(renderProps, CoreProps.ListItemType.ORDERED);
                    CoreProps.ORDERED_LIST_ITEM_NUMBER.set(renderProps, number++);
                } else {
                    CoreProps.LIST_ITEM_TYPE.set(renderProps, CoreProps.ListItemType.BULLET);
                    CoreProps.BULLET_LIST_ITEM_LEVEL.set(renderProps, bulletLevel);
                }

                SpannableBuilder.setSpans(
                        visitor.builder(),
                        spanFactory.getSpans(configuration, renderProps),
                        child.start(),
                        child.end());
            }
        }
    }

    @NonNull
    @Override
    public Collection<String> supportedTags() {
        return Arrays.asList("ol", "ul");
    }

    private static int currentBulletListLevel(@NonNull HtmlTag.Block block) {
        int level = 0;
        while ((block = block.parent()) != null) {
            if ("ul".equals(block.name())
                    || "ol".equals(block.name())) {
                level += 1;
            }
        }
        return level;
    }
}
