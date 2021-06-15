#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

mvn -s .github/maven-ci-settings.xml javadoc:javadoc
cp -R target/site/apidocs docs/

PROJECT_VERSION=$(cat .github/project.yml | yq eval '.release.current-version' -)

mike deploy --push --force --update-aliases $PROJECT_VERSION latest
mike set-default --push --force latest