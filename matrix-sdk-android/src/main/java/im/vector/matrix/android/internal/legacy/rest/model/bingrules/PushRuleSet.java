/* 
 * Copyright 2014 OpenMarket Ltd
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
package im.vector.matrix.android.internal.legacy.rest.model.bingrules;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class PushRuleSet {
    public List<BingRule> override;
    public List<ContentRule> content;
    public List<BingRule> room;
    public List<BingRule> sender;
    public List<BingRule> underride;

    /**
     * Constructor
     */
    public PushRuleSet() {
        override = new ArrayList<>();
        content = new ArrayList<>();
        room = new ArrayList<>();
        sender = new ArrayList<>();
        underride = new ArrayList<>();
    }

    /**
     * Find a rule from its rule ID.
     *
     * @param rules  the rules list.
     * @param ruleID the rule ID.
     * @return the bing rule if it exists, else null.
     */
    private BingRule findRule(List<BingRule> rules, String ruleID) {
        for (BingRule rule : rules) {
            if (TextUtils.equals(ruleID, rule.ruleId)) {
                return rule;
            }
        }
        return null;
    }

    private List<BingRule> getBingRulesList(String kind) {
        List<BingRule> res = null;

        if (BingRule.KIND_OVERRIDE.equals(kind)) {
            res = override;
        } else if (BingRule.KIND_ROOM.equals(kind)) {
            res = room;
        } else if (BingRule.KIND_SENDER.equals(kind)) {
            res = sender;
        } else if (BingRule.KIND_UNDERRIDE.equals(kind)) {
            res = underride;
        }

        return res;
    }

    /**
     * Add a rule from the bingRules
     *
     * @param rule the rule to add.
     */
    public void addAtTop(BingRule rule) {
        if (TextUtils.equals(BingRule.KIND_CONTENT, rule.kind)) {
            if (null != content) {
                if (rule instanceof ContentRule) {
                    content.add(0, (ContentRule) rule);
                }
            }
        } else {
            List<BingRule> rulesList = getBingRulesList(rule.kind);

            if (null != rulesList) {
                rulesList.add(0, rule);
            }
        }
    }

    /**
     * Remove a rule from the bingRules
     *
     * @param rule the rule to delete.
     * @return true if the rule has been deleted
     */
    public boolean remove(BingRule rule) {
        boolean res = false;

        if (BingRule.KIND_CONTENT.equals(rule.kind)) {
            if (null != content) {
                res = content.remove(rule);
            }
        } else {
            List<BingRule> rulesList = getBingRulesList(rule.kind);

            if (null != rulesList) {
                res = rulesList.remove(rule);
            }
        }

        return res;
    }

    /**
     * Find a rule from its rule ID.
     *
     * @param rules  the rules list.
     * @param ruleID the rule ID.
     * @return the bing rule if it exists, else null.
     */
    private BingRule findContentRule(List<ContentRule> rules, String ruleID) {
        for (BingRule rule : rules) {
            if (TextUtils.equals(ruleID, rule.ruleId)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * Find a rule from its ruleID.
     *
     * @param ruleId a RULE_ID_XX value
     * @return the matched bing rule or null it doesn't exist.
     */
    public BingRule findDefaultRule(String ruleId) {
        BingRule rule = null;

        // sanity check
        if (null != ruleId) {
            if (TextUtils.equals(BingRule.RULE_ID_CONTAIN_USER_NAME, ruleId)) {
                rule = findContentRule(content, ruleId);
            } else {
                // assume that the ruleId is unique.
                rule = findRule(override, ruleId);

                if (null == rule) {
                    rule = findRule(underride, ruleId);
                }
            }
        }

        return rule;
    }

    /**
     * Return the content rules list.
     *
     * @return the content rules list.
     */
    public List<BingRule> getContentRules() {
        List<BingRule> res = new ArrayList<>();

        if (null != content) {
            for (BingRule rule : content) {
                if (!rule.ruleId.startsWith(".m.")) {
                    res.add(rule);
                }
            }
        }

        return res;
    }

    /**
     * Return the room rules list.
     *
     * @return the room rules list.
     */
    public List<BingRule> getRoomRules() {
        if (null == room) {
            return new ArrayList<>();
        } else {
            return room;
        }
    }

    /**
     * Return the room rules list.
     *
     * @return the sender rules list.
     */
    public List<BingRule> getSenderRules() {
        if (null == sender) {
            return new ArrayList<>();
        } else {
            return sender;
        }
    }
}
