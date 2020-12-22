#!/bin/bash

mydir="$(dirname "$(realpath "$0")")"

pushd "$mydir" > /dev/null

# Require clean git state
uncommitted=`git status --porcelain`
if [ ! -z "$uncommitted" ]; then
    echo "Uncommitted changes are present, please commit first!"
    exit 1
fi

mydir="."

# Element -> SchildiChat
find "$mydir/vector/src/main/res" -name strings.xml -exec \
    sed -i 's|Element|SchildiChat|g' '{}' \;
# Restore Element where it makes sense
find "$mydir/vector/src/main/res" -name strings.xml -exec \
    sed -i 's/SchildiChat \(Web\|iOS\|Desktop\)/Element \1/g' '{}' \;
find "$mydir/vector/src/main/res" -name strings.xml -exec \
    sed -i 's|SchildiChat Matrix Services|Element Matrix Services|g' '{}' \;
find "$mydir/vector/src/main/res" -name strings.xml -exec \
    sed -i 's|\("use_latest_riot">.*\)SchildiChat\(.*</string>\)|\1Element\2|g' '{}' \;
find "$mydir/vector/src/main/res" -name strings.xml -exec \
    sed -i 's|\("use_other_session_content_description">.*\)SchildiChat\(.*SchildiChat.*</string>\)|\1SchildiChat/Element\2|' '{}' \;

# Requires manual intervention for correct grammar
for strings_de in "$mydir/vector/src/main/res/values-de/strings.xml" "$mydir/matrix-sdk-android/src/main/res/values-de/strings.xml"; do
sed -i 's|!nnen|wolpertinger|g' "$strings_de"
sed -i 's|/innen|wolpertinger|g' "$strings_de"
sed -i 's|!n|schlumpfwesen|g' "$strings_de"
sed -i 's|/in|schlumpfwesen|g' "$strings_de"
# Automated manual intervention:
sed -i 's|da der/die Benutzerschlumpfwesen dasselbe Berechtigungslevel wie du erhalten wirst|da der Benutzer dasselbe Berechtigungslevel wie du erhalten wird|g' "$strings_de"
sed -i 's|des/der anderen Nutzerschlumpfwesen|der anderen Nutzer|g' "$strings_de"
sed -i 's|Nur du und der/die Empfängerwolpertinger haben die Schlüssel um|Nur du und der/die Empfänger haben die Schlüssel, um|g' "$strings_de"
sed -i 's|Nur du und der/die Empfängerschlumpfwesen haben|Nur du und der Empfänger haben|g' "$strings_de"
sed -i 's|kann ein/e Angreiferschlumpfwesen versuchen auf|kann ein Angreifer versuchen, auf|g' "$strings_de"
sed -i 's|wenn du dem/r Besitzerschlumpfwesen|wenn du dem Besitzer|g' "$strings_de"
sed -i 's|Nur für Entwicklerwolpertinger|Nur für Entwickler|g' "$strings_de"
sed -i 's|Verifiziere diese/n Benutzerschlumpfwesen|Verifiziere diesen Benutzer|g' "$strings_de"
sed -i 's|dass ein/e Benutzerschlumpfwesen vertrauenswürdig ist|dass ein Benutzer vertrauenswürdig ist|g' "$strings_de"
sed -i 's|"room_member_power_level_users">Nutzerschlumpfwesen<|"room_member_power_level_users">Nutzer<|g' "$strings_de"
sed -i 's|andere Benutzerwolpertinger sehen|andere Benutzer sehen|g' "$strings_de"
sed -i 's|Andere Benutzerwolpertinger vertrauen|Andere Benutzer vertrauen|g' "$strings_de"
sed -i 's|wird den/die Benutzerschlumpfwesen von diesem Raum ausschließen|wird den Benutzer von diesem Raum ausschließen|g' "$strings_de"
sed -i 's|Um einen erneuten Beitritt zu verhindern, solltest du ihn/sie|Um einen erneuten Beitritt zu verhindern, solltest du ihn|g' "$strings_de"
sed -i 's|\(Du wirst ohne .* und vertraute\) Nutzerwolpertinger neu starten|\1 Nutzer neu starten|g' "$strings_de"
sed -i 's|Der Identitätsserver den|Der Identitätsserver, den|g' "$strings_de"
sed -i 's|aktuelle Sitzung gehört dem/der Benutzerschlumpfwesen%|aktuelle Sitzung gehört %|g' "$strings_de"
sed -i 's|sind von Benutzerschlumpfwesen|sind von|g' "$strings_de"
sed -i 's|Vertraue allen Benutzerwolpertinger|Vertraue allen Benutzern|g' "$strings_de"
sed -i 's|Bis diese/r Benutzerschlumpfwesen \(.*\) werden an und von ihr/ihm|Bis dieser Benutzer \1 werden an und von ihm|g' "$strings_de"
sed -i 's|gelöscht vom Benutzerschlumpfwesen|gelöscht vom Benutzer|g' "$strings_de"
sed -i 's|Nutzerschlumpfwesen hinzufügen|Nutzer hinzufügen|g' "$strings_de"
sed -i 's|von anderen Nutzenden|von anderen Nutzern|g' "$strings_de"
sed -i 's|Bekannte Nutzerwolpertinger|Bekannte Nutzer|g' "$strings_de"
sed -i 's|%d Benutzerwolpertinger<|%d Benutzer<|g' "$strings_de"
sed -i 's|%d Benutzerschlumpfwesen<|%d Benutzer<|g' "$strings_de"
sed -i 's|Zum/r normalen Benutzerschlumpfwesen herabstufen|Zum normalen Benutzer herabstufen|g' "$strings_de"
sed -i 's|frage den/die Administratorwolpertinger|frage den Administrator|g' "$strings_de"
sed -i 's|frage den/die Administratorschlumpfwesen|frage den Administrator|g' "$strings_de"
sed -i 's|Bitte den/die Administratorwolpertinger|Bitte den Administrator|g' "$strings_de"
sed -i 's|Bitte den/die Administratorschlumpfwesen|Bitte den Administrator|g' "$strings_de"
sed -i 's|keine weiteren Inhalte dieses/r Nutzersschlumpfwesen sehen|keine weiteren Inhalte dieses Nutzers sehen|g' "$strings_de"
sed -i 's|gelöscht von Benutzerschlumpfwesen,|gelöscht vom Benutzer,|g' "$strings_de"
sed -i 's|Aktivieren Ende-zu-Ende-Verschlüsselung|Ende-zu-Ende-Verschlüsselung aktivieren|g' "$strings_de"
echo "Check for unresolved strings in $strings_de..."
if grep "wolpertinger\|schlumpfwesen" "$strings_de"; then
    echo -e "\033[1;33m""Script outdated, please update manually!""\033[0m"
    exit 1
fi
done

# Remove Triple-T stuff to avoid using them in F-Droid
rm -rf "$mydir/vector/src/main/play/listings"

git add -A
git commit -m "Automatic SchildiChat string correction"

popd > /dev/null

echo "Seems like language is up-to-date :)"
