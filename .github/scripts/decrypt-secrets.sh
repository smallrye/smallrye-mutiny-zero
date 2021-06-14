#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

echo "Decrypting smallrye signature"
gpg --quiet --batch --yes --decrypt --passphrase="${SECRET_PASSPHRASE}" \
    --output smallrye-sign.asc .github/encrypted/smallrye-sign.asc.gpg

echo "Decrypting Maven settings"
gpg --quiet --batch --yes --decrypt --passphrase="${SECRET_PASSPHRASE}" \
    --output maven-settings.xml .github/encrypted/maven-settings.xml.gpg