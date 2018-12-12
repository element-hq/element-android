package im.vector.matrix.android

import okreplay.OkReplayConfig
import okreplay.PermissionRule
import okreplay.RecorderRule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

class OkReplayRuleChainNoActivity(
        private val configuration: OkReplayConfig) {

    fun get(): TestRule {
        return RuleChain.outerRule(PermissionRule(configuration))
                .around(RecorderRule(configuration))
    }
}