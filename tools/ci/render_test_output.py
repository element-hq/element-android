#!/usr/bin/env python3
#
# Renders a list of xml files taken as arguments into GHA-styled messages in groups.
# Explicitly aims not to have any dependencies, to reduce installation load.
# Should just be able to run in the context of your standard github runner.

import sys
import xml.etree.ElementTree as ET

xmlfiles= sys.argv[1:]

for xmlfile in xmlfiles:
    tree = ET.parse(xmlfile)
    
    root = tree.getroot()
    name = root.attrib['name']
    name = root.attrib['time']
    success = int(root.attrib['tests']) - int(root.attrib['failures']) - int(root.attrib['errors'])
    total = int(root.attrib['tests']) - int(root.attrib['skipped'])
    print(f"::group::{name} {success}/{total} in {time}")
    
    for testcase in root:
        if testcase.tag != "testcase":
            continue
        testname = testcase.attrib['classname']
        message = testcase.attrib['name']
        time = testcase.attrib['time']
        child = testcase.find("failure")
        if child is None:
            print(f"{message} in {time}s")
        else:
            print(f"::error file={testname}::{message} in {time}s")
            print(child.text)
    print("::endgroup::")
