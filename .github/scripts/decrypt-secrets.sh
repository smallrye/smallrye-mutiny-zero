#!/usr/bin/env bash

echo "Decrypting smallrye signature"
gpg --quiet --batch --yes --decrypt --passphrase="${SECRET_FILES_PASSPHRASE}" \
    --output smallrye-sign.asc .github/encrypted/smallrye-sign.asc.gpg

echo "Decrypting Maven settings"
gpg --quiet --batch --yes --decrypt --passphrase="${SECRET_FILES_PASSPHRASE}" \
    --output maven-settings.xml github/encrypted/maven-settings.xml.gpg