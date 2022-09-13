#!/usr/bin/env python3
from collections import OrderedDict

import requests
import json
import re
import os
from bs4 import BeautifulSoup

# A list of words to not capitalize in emoji-names
capitalization_exclude = {'with', 'a', 'at', 'of', 'for', 'and', 'over', 'the', 'off', 'on', 'out', 'in', 'but', 'or'}

# Create skeleton of the final json file as a python dictionary:
emoji_picker_datasource = {
    "compressed": True,
    "categories": [],
    "emojis": {},
    "aliases": {}
}
emoji_picker_datasource_categories = emoji_picker_datasource["categories"]
emoji_picker_datasource_emojis = emoji_picker_datasource["emojis"]


# Get official emoji list from unicode.org (Emoji List, v13.1 at time of writing)
print("Fetching emoji list from Unicode.org...")
req = requests.get("https://unicode.org/emoji/charts/emoji-list.html")
soup = BeautifulSoup(req.content, 'html.parser')

# Navigate to table
table = soup.body.table

# Go over all rows
print("Extracting emojis...")
for row in table.find_all('tr'):
    # Add "bigheads"  rows to categories
    if 'bighead' in next(row.children)['class']:
        relevant_element = row.find('a')
        category_id = relevant_element['name']
        category_name = relevant_element.text
        emoji_picker_datasource_categories.append({
            "id": category_id,
            "name": category_name,
            "emojis": []
        })

    # Add information in "rchars" rows to the last encountered category and emojis
    if row.find('td', class_='code'):
        # Get columns
        cols = row.find_all('td')
        no_element = cols[0]
        code_element = cols[1]
        sample_element = cols[2]
        cldr_element = cols[3]
        keywords_element = cols[4]

        # Extract information from columns
        # Extract name and id
        # => Remove spaces, colons and unicode-characters
        emoji_name = cldr_element.text
        emoji_id = cldr_element.text.lower()
        emoji_id = re.sub(r'[^A-Za-z0-9 ]+', '', emoji_id, flags=re.UNICODE)  # Only keep alphanumeric, space characters
        emoji_id = emoji_id.strip()  # Remove leading/trailing whitespaces
        emoji_id = emoji_id.replace(' ', '-')

        # Capitalize name according to the same rules as the previous emoji_picker_datasource.json
        # - Words are separated by any non-word character (\W), e.g. space, comma, parentheses, dots, etc.
        # - Words are capitalized if they are either at the beginning of the name OR not in capitalization_exclude (extracted from the previous datasource, too)
        emoji_name_cap = "".join([w.capitalize() if i == 0 or w not in capitalization_exclude else w for i, w in enumerate(re.split('(\W)', emoji_name))])

        # Extract emoji unicode-codepoint
        emoji_code_raw = code_element.text
        emoji_code_list = emoji_code_raw.split(" ")
        emoji_code_list = [e[2:] for e in emoji_code_list]
        emoji_code = "-".join(emoji_code_list)

        # Extract keywords
        emoji_keywords = keywords_element.text.split(" | ")

        # Add the emoji-id to the last entry in "categories"
        emoji_picker_datasource_categories[-1]["emojis"].append(emoji_id)

        # Add the emoji itself to the "emojis" dict
        emoji_picker_datasource_emojis[emoji_id] = {
                "a": emoji_name_cap,
                "b": emoji_code,
                "j": emoji_keywords
        }

# The keywords of unicode.org are usually quite sparse.
# There is no official specification of keywords beyond that, but muan/emojilib maintains a well maintained and
# established repository with additional keywords. We extend our list with the keywords from there.
# At the time of writing it had additional keyword information for all emojis except a few from the newest unicode 13.1.
print("Fetching additional keywords from Emojilib...")
req = requests.get("https://raw.githubusercontent.com/muan/emojilib/main/dist/emoji-en-US.json")
emojilib_data = json.loads(req.content)

# We just go over all the official emojis from unicode, and add the keywords there
print("Adding keywords to emojis...")
for emoji in emoji_picker_datasource_emojis:
    emoji_name = emoji_picker_datasource_emojis[emoji]["a"]
    emoji_code = emoji_picker_datasource_emojis[emoji]["b"]

    # Convert back to actual unicode emoji
    emoji_unicode = ''.join(map(lambda s: chr(int(s, 16)), emoji_code.split("-")))

    # Search for emoji in emojilib
    if emoji_unicode in emojilib_data:
        emoji_additional_keywords = emojilib_data[emoji_unicode]
    elif emoji_unicode+chr(0xfe0f)  in emojilib_data:
        emoji_additional_keywords = emojilib_data[emoji_unicode+chr(0xfe0f)]
    else:
        print("* No additional keywords for", emoji_unicode, emoji_picker_datasource_emojis[emoji])
        continue

    # If additional keywords exist, add them to emoji_picker_datasource_emojis
    # Avoid duplicates and keep order. Put official unicode.com keywords first and extend up with emojilib ones.
    new_keywords = OrderedDict.fromkeys(emoji_picker_datasource_emojis[emoji]["j"] + emoji_additional_keywords)
    # Remove the ones derived from the unicode name
    for keyword in [emoji.replace("-", "_")] + [emoji.replace("-", " ")] + [emoji_name]:
        if keyword in new_keywords:
            new_keywords.pop(keyword)
    # Write new keywords back
    emoji_picker_datasource_emojis[emoji]["j"] = list(new_keywords.keys())

# Filter out components from unicode 13.1 (as they are not suitable for single-emoji reactions)
emoji_picker_datasource['categories'] = [x for x in emoji_picker_datasource['categories'] if x['id'] != 'component']

# Write result to file (overwrite previous), without escaping unicode characters
print("Writing emoji_picker_datasource.json...")
scripts_dir = os.path.dirname(os.path.abspath(__file__))
with open(os.path.join(scripts_dir, "../vector/src/main/res/raw/emoji_picker_datasource.json"), "w") as outfile:
    json.dump(emoji_picker_datasource, outfile, ensure_ascii=False, separators=(',', ':'))

# Also export a formatted version
print("Writing emoji_picker_datasource_formatted.json...")
with open(os.path.join(scripts_dir, "../tools/emojis/emoji_picker_datasource_formatted.json"), "w") as outfile:
    json.dump(emoji_picker_datasource, outfile, ensure_ascii=False, indent=4)

print("Done.")
