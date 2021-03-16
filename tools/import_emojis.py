import requests
import json
import re
from bs4 import BeautifulSoup

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
req = requests.get("https://unicode.org/emoji/charts/emoji-list.html")
soup = BeautifulSoup(req.content, 'html.parser')

# Navigate to table
table = soup.body.table

# Go over all rows
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
                "a": emoji_name,
                "b": emoji_code,
                "j": emoji_keywords
        }

# Print result to file (overwrite previous), without escaping unicode characters
with open("../vector/src/main/res/raw/emoji_picker_datasource.json", "w") as outfile:
    json.dump(emoji_picker_datasource, outfile, ensure_ascii=False)
