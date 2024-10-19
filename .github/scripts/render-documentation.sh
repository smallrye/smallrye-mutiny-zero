#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

./mvnw -s .github/maven-ci-settings.xml package javadoc:aggregate -DskipTests
cp -R target/reports/apidocs docs/

PROJECT_VERSION=$(cat .github/project.yml | yq eval '.release.current-version' -)

mike deploy --push --update-aliases $PROJECT_VERSION latest
mike set-default --push latest