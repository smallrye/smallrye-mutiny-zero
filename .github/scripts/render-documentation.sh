#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

./mvnw -s .github/maven-ci-settings.xml package javadoc:aggregate -DskipTests
cp -R target/reports/apidocs docs/

PROJECT_VERSION=$(cat .github/project.yml | yq eval '.release.current-version' -)

uv sync --all-extras --dev
uv run mkdocs build
uv run mike deploy --push --update-aliases $PROJECT_VERSION latest
uv run mike set-default --push latest