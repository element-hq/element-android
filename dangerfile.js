'use strict';

const {danger, fail, message, warn} = require('danger');
const includes = require('lodash.includes');

// This dangerfile contains checks to run after tests are successful, it includes those 
// that run as part of the initial checks, and executes them again.

// For instance, sign-off and changelog files are expected to be created before tests are run
// otherwise we spend 45min running tests again after a tiny change to sign commits or add changelog file, which burns minutes.

// At present, this is empty

message("Coverage and tests are complete")


// Include initial tests again (which will replace the comment with this additional information)
import "./dangerfile.lite"
