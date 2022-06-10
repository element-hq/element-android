'use strict';

const {danger, fail, message, warn} = require('danger');
const includes = require('lodash.includes');

// This file contains checks to be run before expensive tests are executed.
// This allows us to fail early with linting failures or missing changelogs
// and give rapid feedback on PR issues

// This will be re-run after the tests are executed to run any additional checks that
// need the full context.

// Check for PR being branch against develop
// Or maybe against release candidate branch too?

// Check for sign-off if not part of named team members where it's included as part of 
// contractual agreements

// Check for towncrier file matching this PR number

// Check for large PR

message("Waiting for tests and coverage to be generated")
