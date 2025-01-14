#!/usr/bin/env python3

# Copyright 2020-2024 New Vector Ltd.
#
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
# Please see LICENSE files in the repository root for full details.

import argparse
import json
import os
import os.path
# Run `pip3 install requests` if not installed yet
import requests

### Arguments

parser = argparse.ArgumentParser(description='Download sas string from matrix-doc.')
parser.add_argument('-v',
                    '--verbose',
                    help="increase output verbosity.",
                    action="store_true")

args = parser.parse_args()

if args.verbose:
    print("Argument:")
    print(args)

base_url = "https://raw.githubusercontent.com/matrix-org/matrix-spec/main/data-definitions/sas-emoji.json"

print("Downloading " + base_url + "…")

r0 = requests.get(base_url)
data0 = json.loads(r0.content.decode())

if args.verbose:
    print("Json data:")
    print(data0)

print()

# emoji -> translation
default = dict()
# Language -> emoji -> translation
cumul = dict()

for emoji in data0:
    description = emoji["description"]
    if args.verbose:
        print("Description: " + description)
    default[description] = description

    for lang in emoji["translated_descriptions"]:
        if args.verbose:
            print("Lang: " + lang)
        if not (lang in cumul):
            cumul[lang] = dict()
        cumul[lang][description] = emoji["translated_descriptions"][lang]

if args.verbose:
    print(default)
    print(cumul)

def write_file(file, dict):
    print("Writing file " + file)
    if args.verbose:
        print("With")
        print(dict)
    os.makedirs(os.path.dirname(file), exist_ok=True)
    with open(file, mode="w", encoding="utf8") as o:
        o.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        o.write("<resources>\n")
        o.write("    <!-- Generated file, do not edit -->\n")
        for key in dict:
            if dict[key] is None:
                continue
            o.write("    <string name=\"verification_emoji_" + key.lower().replace(" ", "_") + "\">" + dict[key].replace("'", "\\'") + "</string>\n")
        o.write("</resources>\n")

scripts_dir = os.path.dirname(os.path.abspath(__file__))
data_defs_dir = os.path.join(scripts_dir, "../matrix-sdk-android/src/main/res")

# Write default file
write_file(os.path.join(data_defs_dir, "values/strings_sas.xml"), default)

# Write each language file
for lang in cumul:
    androidLang = lang\
        .replace("_", "-r")\
        .replace("zh-rHans", "zh-rCN") \
        .replace("zh-rHant", "zh-rTW")
    write_file(os.path.join(data_defs_dir, "values-" + androidLang + "/strings_sas.xml"), cumul[lang])

print()
print("Success!")
