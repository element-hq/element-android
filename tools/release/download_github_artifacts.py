#!/usr/bin/env python3
#
# Copyright 2022 New Vector Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import argparse
import hashlib
import json
import os
# Run `pip3 install requests` if not installed yet
import requests

# This script downloads artifacts from GitHub.
# Ref: https://docs.github.com/en/rest/actions/artifacts#get-an-artifact

error = False

### Arguments

parser = argparse.ArgumentParser(description='Download artifacts from GitHub.')
parser.add_argument('-t',
                    '--token',
                    required=True,
                    help='The GitHub token with read access.')
parser.add_argument('-a',
                    '--artifactUrl',
                    required=True,
                    help='the artifact_url from GitHub.')
parser.add_argument('-f',
                    '--filename',
                    help='the filename, if not provided, will use the artifact name.')
parser.add_argument('-i',
                    '--ignoreErrors',
                    help='Ignore errors that can be ignored. Build state and number of artifacts.',
                    action="store_true")
parser.add_argument('-d',
                    '--directory',
                    default="",
                    help='the target directory, where files will be downloaded. If not provided the build number will be used to create a directory.')
parser.add_argument('-v',
                    '--verbose',
                    help="increase output verbosity.",
                    action="store_true")
parser.add_argument('-s',
                    '--simulate',
                    help="simulate action, do not create folder or download any file.",
                    action="store_true")

args = parser.parse_args()

if args.verbose:
    print("Argument:")
    print(args)

# Split the artifact URL to get information
# Ex: https://github.com/vector-im/element-android/suites/9293388174/artifacts/435942121
artifactUrl = args.artifactUrl
if not artifactUrl.startswith('https://github.com/'):
    print("❌ Invalid parameter --artifactUrl %s. Must start with 'https://github.com/'" % artifactUrl)
    exit(1)
if "/artifacts/" not in artifactUrl:
    print("❌ Invalid parameter --artifactUrl %s. Must contain '/artifacts/'" % artifactUrl)
    exit(1)
artifactItems = artifactUrl.split("/")
if len(artifactItems) != 9:
    print("❌ Invalid parameter --artifactUrl %s. Please check the format." % (artifactUrl))
    exit(1)

gitHubRepoOwner = artifactItems[3]
gitHubRepo = artifactItems[4]
artifactId = artifactItems[8]

if args.verbose:
    print("gitHubRepoOwner: %s, gitHubRepo: %s, artifactId: %s" % (gitHubRepoOwner, gitHubRepo, artifactId))

headers = {
   'Authorization': "Bearer %s" % args.token,
   'Accept': 'application/vnd.github+json'
}
base_url = "https://api.github.com/repos/%s/%s/actions/artifacts/%s" % (gitHubRepoOwner, gitHubRepo, artifactId)

### Fetch build state

print("Getting artifacts data of project '%s/%s' artifactId '%s'..." % (gitHubRepoOwner, gitHubRepo, artifactId))

if args.verbose:
    print("Url: %s" % base_url)

r = requests.get(base_url, headers=headers)
data = json.loads(r.content.decode())

if args.verbose:
    print("Json data:")
    print(data)

if args.verbose:
    print("Create subfolder %s to download artifacts..." % artifactId)

if args.directory == "":
    targetDir = artifactId
else:
    targetDir = args.directory

if not args.simulate:
    os.makedirs(targetDir, exist_ok=True)

url = data.get("archive_download_url")
if args.filename is not None:
    filename = args.filename
else:
    filename = data.get("name") + ".zip"

## Print some info about the artifact origin
commitLink = "https://github.com/%s/%s/commit/%s" % (gitHubRepoOwner, gitHubRepo, data.get("workflow_run").get("head_sha"))
print("Preparing to download artifact `%s`, built from branch: `%s` (commit %s)" % (data.get("name"), data.get("workflow_run").get("head_branch"), commitLink))

if args.verbose:
    print()
    print("Artifact url: %s" % url)

target = targetDir + "/" + filename
sizeInBytes = data.get("size_in_bytes")
print("Downloading %s to '%s' (file size is %s bytes, this may take a while)..." % (filename, targetDir, sizeInBytes))
if not args.simulate:
    # open file to write in binary mode
    with open(target, "wb") as file:
        # get request
        response = requests.get(url, headers=headers)
        # write to file
        file.write(response.content)
    print("Verifying file size...")
    # get the file size
    size = os.path.getsize(target)
    if sizeInBytes != size:
        # error = True
        print("Warning, file size mismatch: expecting %s and get %s. This is just a warning for now..." % (sizeInBytes, size))

if error:
    print("❌ Error(s) occurred, please check the log")
    exit(1)
else:
    print("Done!")
