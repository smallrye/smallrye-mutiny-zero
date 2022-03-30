#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

echo "Set up Git"

git config --global user.name "SmallRye CI"
git config --global user.email "smallrye@googlegroups.com"

echo "Import key"

gpg --fast-import --no-tty --batch --yes smallrye-sign.asc

echo "Fetch code"

git fetch origin --tags
git reset --hard
git checkout main

echo "Deploy"

./mvnw -B clean deploy -DskipTests -Prelease -s maven-settings.xml

