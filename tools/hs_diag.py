#!/usr/bin/env python3

#  Copyright (c) 2020 New Vector Ltd
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import argparse
import os

### Arguments

parser = argparse.ArgumentParser(description='Get some information about a homeserver.')
parser.add_argument('-s',
                    '--homeserver',
                    required=True,
                    help="homeserver URL")
parser.add_argument('-v',
                    '--verbose',
                    help="increase output verbosity.",
                    action="store_true")

args = parser.parse_args()

if args.verbose:
    print("Argument:")
    print(args)

baseUrl = args.homeserver

if not baseUrl.startswith("http"):
    baseUrl = "https://" + baseUrl

if not baseUrl.endswith("/"):
    baseUrl = baseUrl + "/"

print("Get information from " + baseUrl)

items = [
    # [Title, URL, True for GET request and False for POST request]
    ["Well-known", baseUrl + ".well-known/matrix/client", True]
    , ["API version", baseUrl + "_matrix/client/versions", True]
    , ["Homeserver version", baseUrl + "_matrix/federation/v1/version", True]
    , ["Login flow", baseUrl + "_matrix/client/r0/login", True]
    , ["Registration flow", baseUrl + "_matrix/client/r0/register", False]
    # Useless , ["Username availability", baseUrl + "_matrix/client/r0/register/available?username=benoit", True]
    # Useless , ["Public rooms", baseUrl + "_matrix/client/r0/publicRooms?limit=1", True]
    # Useless , ["Profile", baseUrl + "_matrix/client/r0/profile/@benoit.marty:matrix.org", True]
    # Need token , ["Capability", baseUrl + "_matrix/client/r0/capabilities", True]
    # Need token , ["Media config", baseUrl + "_matrix/media/r0/config", True]
    # Need token , ["Turn", baseUrl + "_matrix/client/r0/voip/turnServer", True]
]

for item in items:
    print("====================================================================================================")
    print("# " + item[0] + " (" + item[1] + ")")
    print("====================================================================================================")
    if item[2]:
        os.system("curl -s -X GET '" + item[1] + "' | python3 -m json.tool")
    else:
        os.system("curl -s -X POST --data $'{}' '" + item[1] + "' | python3 -m json.tool")
