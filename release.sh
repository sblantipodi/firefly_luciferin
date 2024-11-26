#!/bin/bash

echo "Please enter the release tag, this will be used by the CI to create the release (ex: 2.17.10): "
read -p "-> " input_string
echo "Creating tag: $input_string"
#git tag -a "v$tag_version" -m "v$tag_version";
#git push origin --tags;

# Chiedi all'utente di premere un tasto per proseguire
read -p "GitHub Actions is building the project:"
read -p "https://github.com/sblantipodi/firefly_luciferin/actions"
read -p "Press enter to continue the release on FlatHub once finished..."

# Esegui il secondo comando
mkdir ../tmp_remove;
cd ../tmp_remove;
wget https://dpsoftware.org/2.17.10/FireflyLuciferinLinux.deb;
sha256_value=$(sha256sum FireflyLuciferinLinux.deb | awk '{ print $1 }');
git clone git@github.com:flathub/org.dpsoftware.FireflyLuciferin.git;
cd org.dpsoftware.FireflyLuciferin;
git checkout -b new-br
cp ../fireflyluciferin/flathub/org.dpsoftware.FireflyLuciferin.json .;
jq --arg sha256 "$sha256_value" '( .modules[] | select(type == "object" and .name == "fireflyluciferin") | .sources[0] ) |= (.sha256 = $sha256 | .url = "https://dpsoftware.org/$input_string/FireflyLuciferinLinux.deb")' org.dpsoftware.FireflyLuciferin.json > temp.json && mv temp.json org.dpsoftware.FireflyLuciferin.json;
cat org.dpsoftware.FireflyLuciferin.json;
git add org.dpsoftware.FireflyLuciferin.json
git commit -m "bot, build"
git push origin new-br
gh pr create --title "Bot release" --body "This release is made by DPsoftware gentle bot" --base master --head new-br
pr_number=$(gh pr list --state open --json number --jq '.[0].number')
echo "A pull request has been created on FlatHub. Please visit:";
echo "https://github.com/flathub/org.dpsoftware.FireflyLuciferin/pulls";
echo "to merge it."
cd ..;
rm -rf tmp_remove;

