package im.vector.matrix.android.internal.session.notification

import arrow.core.Try
import im.vector.matrix.android.api.pushrules.rest.PushRule
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.session.pushers.DefaultConditionResolver
import im.vector.matrix.android.internal.task.Task
import timber.log.Timber

internal interface ProcessEventForPushTask : Task<ProcessEventForPushTask.Params, Unit> {
    data class Params(
            val events: List<Event>,
            val rules: List<PushRule>
    )
}

internal class DefaultProcessEventForPushTask(
        private val defaultPushRuleService: DefaultPushRuleService
) : ProcessEventForPushTask {


    override suspend fun execute(params: ProcessEventForPushTask.Params): Try<Unit> {
        return Try {
            params.events.forEach { event ->
                fulfilledBingRule(event, params.rules)?.let {
                    Timber.v("Rule $it match for event ${event.eventId}")
                    defaultPushRuleService.dispatchBing(event, it)
                }
            }
            defaultPushRuleService.dispatchFinish()
        }
    }

    private fun fulfilledBingRule(event: Event, rules: List<PushRule>): PushRule? {
        val conditionResolver = DefaultConditionResolver(event)
        rules.filter { it.enabled }.forEach { rule ->
            val isFullfilled = rule.conditions?.map {
                it.asExecutableCondition()?.isSatisfied(conditionResolver) ?: false
            }?.fold(true/*A rule with no conditions always matches*/, { acc, next ->
                //All conditions must hold true for an event in order to apply the action for the event.
                acc && next
            }) ?: false

            if (isFullfilled) {
                return rule
            }
        }
        return null
    }

}