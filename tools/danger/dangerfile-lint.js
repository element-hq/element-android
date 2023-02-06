import { schedule } from 'danger'

/**
 * Ref and documentation: https://github.com/damian-burke/danger-plugin-lint-report
 * This file will check all the error in XML Checkstyle format.
 * It covers, lint, ktlint, and detekt errors
 */

const reporter = require("danger-plugin-lint-report")
schedule(reporter.scan({
    /**
     * File mask used to find XML checkstyle reports.
     */
    fileMask: "**/reports/**/**.xml",
    /**
     * If set to true, the severity will be used to switch between the different message formats (message, warn, fail).
     */
    reportSeverity: true,
    /**
     * If set to true, only issues will be reported that are contained in the current changeset (line comparison).
     * If set to false, all issues that are in modified files will be reported.
     */
    requireLineModification: false,
    /**
     * Optional: Sets a prefix foreach violation message.
     * This can be useful if there are multiple reports being parsed to make them distinguishable.
     */
     // outputPrefix?: ""
}))
