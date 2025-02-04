#!/usr/bin/env python3
#
# Copyright 2020-2024 New Vector Ltd.
#
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
# Please see LICENSE files in the repository root for full details.
#

import argparse
import hashlib
import json
import os
# Run `pip3 install requests` if not installed yet
import requests

# This script downloads artifacts from buildkite.
# Ref: https://buildkite.com/docs/apis/rest-api/artifacts#download-an-artifact

# Those two variables are specific to the Element Android project
ORG_SLUG = "matrix-dot-org"
PIPELINE_SLUG = "element-android"

### Arguments

parser = argparse.ArgumentParser(description='Download artifacts from Buildkite.')
parser.add_argument('-t',
                    '--token',
                    required=True,
                    help='The buildkite token.')
parser.add_argument('-b',
                    '--build',
                    type=int,
                    required=True,
                    help='the buildkite build number.')
parser.add_argument('-f',
                    '--filename',
                    help='the filename, to download only one artifact.')
parser.add_argument('-e',
                    '--expecting',
                    type=int,
                    help='the expected number of artifacts. If omitted, no check will be done.')
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

# parser has checked that the build was an int, convert to String for the rest of the script
build_str = str(args.build)

if args.verbose:
    print("Argument:")
    print(args)

headers = {'Authorization': "Bearer %s" % args.token}
base_url = "https://api.buildkite.com/v2/organizations/%s/pipelines/%s/builds/%s" % (ORG_SLUG, PIPELINE_SLUG, build_str)

### Fetch build state

buildkite_build_state_url = base_url

buildkite_url = "https://buildkite.com/%s/%s/builds/%s" % (ORG_SLUG, PIPELINE_SLUG, build_str)

print("Getting build state of project %s/%s build %s (%s)" % (ORG_SLUG, PIPELINE_SLUG, build_str, buildkite_url))

if args.verbose:
    print("Url: %s" % buildkite_build_state_url)

r0 = requests.get(buildkite_build_state_url, headers=headers)
data0 = json.loads(r0.content.decode())

if args.verbose:
    print("Json data:")
    print(data0)

print("   git branch         : %s" % data0.get('branch'))
print("   git commit         : \"%s\"" % data0.get('commit'))
print("   git commit message : \"%s\"" % data0.get('message'))
print("   build state        : %s" % data0.get('state'))

error = False

if data0.get('state') != 'passed':
    print("❌ Error, the build is in state '%s', and not 'passed'" % data0.get('state'))
    if args.ignoreErrors:
        error = True
    else:
        exit(1)

### Fetch artifacts list

buildkite_artifacts_url = base_url + "/artifacts"

print("Getting artifacts list of project %s/%s build %s" % (ORG_SLUG, PIPELINE_SLUG, build_str))

if args.verbose:
    print("Url: %s" % buildkite_artifacts_url)

r = requests.get(buildkite_artifacts_url, headers=headers)
data = json.loads(r.content.decode())

print("   %d artifact(s) found." % len(data))

if args.expecting is not None and args.expecting != len(data):
    print("❌ Error, expecting %d artifacts and found %d." % (args.expecting, len(data)))
    if args.ignoreErrors:
        error = True
    else:
        exit(1)

if args.verbose:
    print("Json data:")
    print(data)

if args.verbose:
    print("Create subfolder %s to download artifacts..." % build_str)

if args.directory == "":
    targetDir = build_str
else:
    targetDir = args.directory

if not args.simulate:
    os.makedirs(targetDir, exist_ok=True)

for elt in data:
    if args.verbose:
        print()
        print("Artifact info:")
        for key, value in elt.items():
            print("   %s: %s" % (key, str(value)))
    url = elt.get("download_url")
    filename = elt.get("filename")
    if args.filename is not None and args.filename != filename:
        continue
    target = targetDir + "/" + filename
    print("Downloading %s to '%s'..." % (filename, targetDir))
    if not args.simulate:
        # open file to write in binary mode
        with open(target, "wb") as file:
            # get request
            response = requests.get(url, headers=headers)
            # write to file
            file.write(response.content)
        print("Verifying checksum...")
        # open file to read in binary mode
        with open(target, "rb") as file:
            data = file.read()
            hash = hashlib.sha1(data).hexdigest()
        if elt.get("sha1sum") != hash:
            error = True
            print("❌ Checksum mismatch: expecting %s and get %s" % (elt.get("sha1sum"), hash))

if error:
    print("❌ Error(s) occurred, please check the log")
    exit(1)
else:
    print("Done!")
