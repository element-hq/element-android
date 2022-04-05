#!/usr/bin/env python3
#
# Renders a list of xml files taken as arguments into GHA-styled messages in groups.
# Explicitly aims not to have any dependencies, to reduce installation load.
# Should just be able to run in the context of your standard github runner.

# Potentially rewrite as an independent action, use handlebars to template result
import sys
import xml.etree.ElementTree as ET
suitename = sys.argv[1]
xmlfiles = sys.argv[2:]
print("::group::Arguments")
print(f"{sys.argv}") 
print("::endgroup::")
for xmlfile in xmlfiles:
    try:
        tree = ET.parse(xmlfile)
    
        root = tree.getroot()
        name = root.attrib['name']
        time = root.attrib['time']
        tests = int(root.attrib['tests'])
        skipped = int(root.attrib['skipped'])
        errors = int(root.attrib['errors'])
        failures = int(root.attrib['failures'])
        success = tests - failures - errors - skipped
        total = tests - skipped
        print(f"::group::{name} {success}/{total} ({skipped} skipped) in {time}")
        
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
        body = f" passed={success} failures={failures} errors={errors} skipped={skipped}"
        print(f"::set-output name={suitename}::={body}")
    except FileNotFoundError:
        print(f"::error::Unable to open test results file {xmlfile} - check if the tests completed")
    print("::endgroup::")

