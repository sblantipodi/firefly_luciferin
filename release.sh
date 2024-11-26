#!/bin/bash

read -p "Please enter the release tag, this will be used by the CI to create the release: " input_string
echo "Creating tag: $input_string"
#git tag -a "v$tag_version" -m "v$tag_version";
#git push origin --tags;

# Chiedi all'utente di premere un tasto per proseguire
read -p "GitHub Actions is building the project. Press enter to continue the release on FlatHub..."

# Esegui il secondo comando
cd build_tools/flatpak/org.dpsoftware.FireflyLuciferin;
wget https://dpsoftware.org/2.17.9/FireflyLuciferinLinux.deb;
sha256_value=$(sha256sum FireflyLuciferinLinux.deb | awk '{ print $1 }');
